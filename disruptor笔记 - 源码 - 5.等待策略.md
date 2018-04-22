# disruptor笔记 - 源码 - 等待策略
- ProcessingSequenceBarrier的waitFor方法
	
	```java
		public long waitFor(final long sequence)
	        throws AlertException, InterruptedException, TimeoutException
	    {
	    		//检查报警状态
	        checkAlert();
	
	        	//根据waitStrategy来等待最大可用序列号的返回
	        long availableSequence = waitStrategy.waitFor(sequence, cursorSequence, dependentSequence, this);
	        
	        //可用序列号比消费者想要消费的序列号小，直接返回可用序列号
	        if (availableSequence < sequence)
	        {
	            return availableSequence;
	        }
	        /*
	         * 否则，要返回能安全使用的最大的序列值。
	         * SingleProducer的情况下直接返回availableSequence；
	         * MultiProducer的情况下需要根据AvailableBuffer来确定位置
	         */
	        return sequencer.getHighestPublishedSequence(sequence, availableSequence);
	    }
	```
	
- 可见等待都是由waitStrategy来完成的，WaitStrategy的接口方法如下

	```java
		/**
	     * Wait for the given sequence to be available.  It is possible for this method to return a value
	     * less than the sequence number supplied depending on the implementation of the WaitStrategy.  A common
	     * use for this is to signal a timeout.  Any EventProcessor that is using a WaitStrategy to get notifications
	     * about message becoming available should remember to handle this case.  The {@link BatchEventProcessor} explicitly
	     * handles this case and will signal a timeout if required.
	     *
	     * @param sequence          to be waited on. 等待(申请)的序列值。
	     * @param cursor            the main sequence from ringbuffer. Wait/notify strategies will
	     *                          need this as it's the only sequence that is also notified upon update.
	     *                          ringBuffer中的主序列，也可以认为是事件发布者使用的序列。 
	     * @param dependentSequence on which to wait.事件处理者使用的序列。 
	     * @param barrier           the processor is waiting on.序列栅栏。 
	     * @return the sequence that is available which may be greater than the requested sequence.
	     * 对事件处理者来说可用的序列值，可能会比申请的序列值大。 
	     * @throws AlertException       if the status of the Disruptor has changed.
	     * @throws InterruptedException if the thread is interrupted.
	     * @throws TimeoutException if a timeout occurs before waiting completes (not used by some strategies)
	     */
	    long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier)
	        throws AlertException, InterruptedException, TimeoutException;
	
	    /**
	     * Implementations should signal the waiting {@link EventProcessor}s that the cursor has advanced.
	     * 当发布事件成功后会调用这个方法来通知等待的事件处理者序列可用了。 
	     */
	    void signalAllWhenBlocking();
	```

- waitStrategy的主要实现
	- BlockingWaitStrategy
		- BlockingWaitStrategy的实现方法是阻塞等待。当要求节省CPU资源，而不要求高吞吐量和低延迟的时候使用这个策略。
	- BusySpinWaitStrategy
		- BusySpinWaitStrategy的实现方法是自旋等待。这种策略会利用CPU资源来避免系统调用带来的延迟抖动，当线程可以绑定到指定CPU(核)的时候可以使用这个策略。
	- LiteBlockingWaitStrategy
		- LiteBlockingWaitStrategy的实现方法也是阻塞等待，但它会减少一些不必要的唤醒。
	- SleepingWaitStrategy
		- SleepingWaitStrategy的实现方法是先自旋，不行再临时让出调度(yield)，不行再短暂的阻塞等待。对于既想取得高性能，由不想太浪费CPU资源的场景，这个策略是一种比较好的折中方案。使用这个方案可能会出现延迟波动。
	- TimeoutBlockingWaitStrategy
		- TimeoutBlockingWaitStrategy的实现方法是阻塞给定的时间，超过时间的话会抛出超时异常。】
	- YieldingWaitStrategy
		- SleepingWaitStrategy的实现方法是先自旋(100次)，不行再临时让出调度(yield)。和SleepingWaitStrategy一样也是一种高性能与CPU资源之间取舍的折中方案，但这个策略不会带来显著的延迟抖动。
	- PhasedBackoffWaitStrategy
		-  PhasedBackoffWaitStrategy的实现方法是先自旋(10000次)，不行再临时让出调度(yield)，不行再使用其他的策略进行等待。可以根据具体场景自行设置自旋时间、yield时间和备用等待策略。
		