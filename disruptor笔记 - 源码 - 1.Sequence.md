# disruptor笔记 - 源码 - Sequence

- Sequence 序列是用来标识生产者和消费者在RingBuffer上的位置的
	- LhsPadding、Value、RhsPadding，总共占据了128个字节来做cache line padding，确保不发生FalseSharing
		- FalseSharing例子 A、B两个线程分别访问的a对象和b对象出现在了同一个cache line内，A线程访问的a对象发生写操作后势必导致cache line的重载，B线程访问的b对象原本可以在cache line里读取，但A线程的操作导致b对象需要重新从主内存中进行读取
	- Value类的value是一个volatile修饰的long
	- 静态初始化了UNSAFE.
	- Sequence是一个做了缓存行填充优化的原子序列。

- FixedSequenceGroup相当于包含了若干序列的一个包装类，尽管本身继承了Sequence，但只是重写了get方法，获取内部序列组中最小的序列值，但其他的"写"方法都不支持。

- Sequencer
	- Sequencer接口扩展了Cursored和Sequenced
		- Cursored提供了一个获取当前游标序列号的方法
		- Sequenced则包含ringbuffer的查询方法、申请节点方法和数据写入方法
			- 比较重要的是 hasAvailableCapacity方法、next方法以及publish方法。
			- hasAvailableCapacity用于判断当前是否有足够的容量来生产数据
			- next方法用于给数据申请槽位，和hasAvailableCapacity在容量判断上有类似的逻辑
			- publish用于申请槽位成功后的数据提交，数据提交完成后会唤醒消费者对数据进行消费
					
		- Sequencer Sequencer接口的很多功能是提供给producer用的
			- 通过Sequencer可以得到一个SequenceBarrier，提供给consumer使用。
   			- Sequencer的实现类有SingleProducerSequencer和MultiProducerSequencer，它们的父类是AbstractSequencer
       		- AbstractSequencer
       			- 管理追踪序列和关联当前序列。
       		- SingleProducerSequencer
       			- hasAvailableCapacity(final int requiredCapacity)
			    		- 当前序列的nextValue + requiredCapacity是producer要申请的序列值。
					- 当前序列的cachedValue记录的是之前consumer申请的序列值。
					- producer要申请的序列值大于consumer之前的序列值 且 producer要申请的序列值减去环的长度要小于consumer的序列值。
						- 如果满足这个条件，即使不知道当前consumer的序列值，也能确保producer可以申请给定的序列。
						- 如果不满足这个条件，就需要查看一下当前consumer的最小的序列值(因为可能有多个consumer)，如果当前要申请的序列值比当前consumer的最小序列值大了一圈(从后面追上了)，那就不能申请了(申请的话会覆盖没被消费的事件)，也就是说没有可用的空间(用来发布事件)了，也就是hasAvailableCapacity方法要表达的意思。
				- next(int n)
					- 申请序列的方法，里面的逻辑和hasAvailableCapacity一样，只是在不能申请序列的时候会阻塞等待一下，然后重试。
				- tryNext(int n)
					- next的非阻塞版本，申请一次不成功就报错
			
			- MultiProducerSequencer
				- 多生产者模式下，由于无法确定消费者是否会读到还没写入的数据的槽位，故设计了availableBuffer
				- availableBuffer是一个与与RingBuffer大小相同的数组，使用availableBuffer来标记写入位置，消费者调用waitFor时会访问availableBuffer来确定最大读取位
			
- SequencerBarrier 消费者使用序列的接口
	
	- SequencerBarrier的主要实现类
		- ProcessingSequenceBarrier
			- 需要关注的方法是waitFor方法，waitFor方法会要求消费者传入一个期望消费的序列号，方法内根据指定的waitStrategy来获取可消费序列号。
				- 如果可消费序号比期望消费的序列号小，则直接返回可消费序列号。
				- 否则则需要计算最大可消费序号
					- SingleProducer的情况下，最大可消费序号即可消费序列号，因为只有一个生产者，每个槽位都消费即可。
					- MultiProducer的情况下，需要根据availableBuffer来确定最大读取位置。

- 总结
	- 真正的序列是Sequence。
	- 事件发布者通过Sequencer的大部分功能来使用序列。
	- 事件处理者通过SequenceBarrier来使用序列。
