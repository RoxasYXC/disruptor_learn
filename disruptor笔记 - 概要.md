# disruptor笔记 - 概要
- RingBuffer
	- 数据结构是个环，实现是数组。
		- 使用数组主要是因为：
			- 数组在内存中的连续性，因为数组会将连续的数据读取到缓存行里，提高读取效率。
			- 数组需要预分配内存，不会因为对象的增删触发GC，从而起到减少GC的效果。
		- RingBuffer中存在一个序号，这个序号指向数组中下一个可用对象。
		- 槽的个数必须是二的N次方，和hashmap一样，提高读取效率，位与操作比取模的效率要高很多。
			- 不是的话会报IllegalArgumentException。
			- 环中的指针所在的槽位index=sequence mod array length

- 生产消费过程			
	- 整个生产消费过程类似于银行取号，办事窗口即RingBuffer的槽位，先拿号（seq），等通知，不是每次叫号就去抢资源。
	- 数据消费的大致过程
		- Consumer处理完RingBuffer中某个序号数据后，调用Barrier(老版本ConsumerBarrier，3里是SequenceBarrier)的waitFor(已消费序号)方法.
		- waitFor()调用返回下一个可以调用的最大序号。
		- 当前Consumer会根据WaitStrategy进入等待的状态。
		- 等待中的Consumer会收到前序序号的数据写入通知，直到最大序号达到后，进行后续的消费动作。
		
	- 数据消费方式的优势
		- 对每个数据的访问不再有锁，只要到达waitfor的seq后取出上次记录的seq和waitfor的seq之间的数据即可。
		
	- 数据生产的大致过程
		- 两步提交 
			- 申请节点
				- 会根据消费者的消费速度来确定下一个节点的位置，如果有消费者即将被套圈（实际永远不会），申请节点的生产者会进入根据用户定义的waitStrategy进入等待状态。
				- 申请节点成功后会将最新的seq序号更新给原来的槽位，开始进入数据写入阶段。
			- 数据写入commit
				- 数据提交完成后，通知consumer的waitStrategy进行数据消费。
				- 多生产者的情况下，由于数据消费的顺序是根据seq来决定的，所以就算持有大seq的生产者已经完成了commit前的所有操作，也必须等待小seq只有的生产者commit前序节点后才能完成数据的commit工作。
	
	- 数据生产的批处理
		- ringBuffer知道最快的消费者和最慢的消费者的进度，所以就等于知道了哪些节点的槽位里是没有待消费数据的，所以可以在不检查消费者位置的情况下进行数据的批量写入
			
