# disruptor笔记 - 源码 - 事件发布
- RingBuffer本身就可以进行事件的发布，前面说到过初始化时会传入EventFactory来进行事件的预填充
- 事件
- 数据
- EventTranslator 
	- 通过实现EventTranslator接口来规范化对事件内数据的转换
	-  EventTranslator有多参形式的
	- 通过EventTranslator并非是必要的
- 总结
	- 明确发布场景，合理的选择发布模式(单线程还是多线程)
	- 错误使用发布模式会导致槽位数据在未消费的情况下被覆盖
- example
	- SingleProducer.java
	- 	MultiProducer.java
		