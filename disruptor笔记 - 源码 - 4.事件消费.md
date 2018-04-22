# disruptor笔记 - 源码 - 事件消费
- 事件处理接口EventProcessor
	- 事件处理器会等待RingBuffer中的事件变为可用(可处理)，然后处理可用的事件。 
	- 一个事件处理器通常会关联一个线程。
	- Sequence getSequence();  获取序列引用
	- void halt();挂起事件
	- boolean isRunning();  事件是否处理中的状态
- EventProcessor的实现类
	- BatchEventProcessor<T> 顾名思义，对事件进行批量处理，多消费者的情况下，每个消费者都会处理到RingBuffer中的每个事件，基于这样的特性可以实现pubsub模式和pipline模式
		- EventHandler 事件处理方法的回调接口
			- void onEvent(T event, long sequence, boolean endOfBatch) throws Exception;
				- event 要处理的事件
				- sequence事件对应的序列号
				- endOfBatch 是否批处理的最后一个事件
		- BatchStartAware 
			- void onBatchStart(long batchSize);
				- batchSize为这次批处理要处理的事件数量
		- TimeoutHandler
			-  void onTimeout(long sequence) throws Exception;
				- sequence事件对应的序列号 
		- run
			
			```java
				public void run()
			    {
			    		//状态设置与检测。
			        if (!running.compareAndSet(false, true))
			        {
			            throw new IllegalStateException("Thread is already running");
			        }
			        //先清除序列栅栏的通知状态。 
			        sequenceBarrier.clearAlert();
			        //如果eventHandler实现了LifecycleAware，这里会对其进行一个启动通知。  
			        notifyStart();
			
			        T event = null;
			        //获取要申请的序列值。  
			        long nextSequence = sequence.get() + 1L;
			        try
			        {
			            while (true)
			            {
			                try
			                {
			                		//通过序列栅栏来等待可用的序列值
			                    final long availableSequence = sequenceBarrier.waitFor(nextSequence);
			                    //如果设置了batchStartAware，会再onEvent之前调用
			                    if (batchStartAware != null)
			                    {
			                        batchStartAware.onBatchStart(availableSequence - nextSequence + 1);
			                    }
			                    	//得到可用的序列值后，批量处理nextSequence到availableSequence之间的事件。
			                    while (nextSequence <= availableSequence)
			                    {
			                        event = dataProvider.get(nextSequence);
			                        eventHandler.onEvent(event, nextSequence, nextSequence == availableSequence);
			                        nextSequence++;
			                    }
			                    //处理完毕后，设置当前处理完成的最后序列值。 
			                    sequence.set(availableSequence);
			                }
			                catch (final TimeoutException e)
			                {
			                		//如果设置了timeoutHandler，在这里调起
			                    notifyTimeout(sequence.get());
			                }
			                catch (final AlertException ex)
			                {
			                		//如果捕获了序列栅栏变更通知，并且当前事件处理器停止了，那么退出主循环。
			                    if (!running.get())
			                    {
			                        break;
			                    }
			                }
			                catch (final Throwable ex)
			                {
			                    exceptionHandler.handleEventException(ex, nextSequence, event);
			                    //处理异常后仍然会设置当前处理的最后的序列值，然后继续处理其他事件。
			                    sequence.set(nextSequence);
			                    nextSequence++;
			                }
			            }
			        }
			        finally
			        {
			        		//主循环退出后，如果eventHandler实现了LifecycleAware，这里会对其进行一个停止通知。  
			            notifyShutdown();
			            //设置事件处理器运行状态为停止。  
			            running.set(false);
			        }
			    }
			```
		- BatchEventProcessor内部会记录自己的序列、运行状态。
		- BatchEventProcessor需要外部提供数据提供者(其实就是队列-RingBuffer)、序列栅栏、异常处理器。
		- BatchEventProcessor其实是将事件委托给内部的EventHandler来处理的。
		
	- WorkProcessor<T> 和BatchEventProcessor最大的区别在于每个事件只会被消费一次
		
		```java
			public void run()
		    {
		    		//状态设置与检测。
		        if (!running.compareAndSet(false, true))
		        {
		            throw new IllegalStateException("Thread is already running");
		        }
		        //先清除序列栅栏的通知状态。  
		        sequenceBarrier.clearAlert();
		        //如果workHandler实现了LifecycleAware，这里会对其进行一个启动通知。  
		        notifyStart();
		
		        boolean processedSequence = true;
		        long cachedAvailableSequence = Long.MIN_VALUE;
		        long nextSequence = sequence.get();
		        T event = null;
		        while (true)
		        {
		            try
		            {
		                // if previous sequence was processed - fetch the next sequence and set
		                // that we have successfully processed the previous sequence
		                // typically, this will be true
		                // this prevents the sequence getting too far forward if an exception
		                // is thrown from the WorkHandler
		            	    //判断上一个事件是否已经处理完毕。
		                if (processedSequence)
		                {
		                		//如果处理完毕，重置标识。 
		                    processedSequence = false;
		                    //原子的获取下一要处理事件的序列值。  
		                    do
		                    {
		                        nextSequence = workSequence.get() + 1L;
		                        sequence.set(nextSequence - 1L);
		                    }
		                    while (!workSequence.compareAndSet(nextSequence - 1L, nextSequence));
		                }
		                //检查序列值是否需要申请。这一步是为了防止和事件生产者冲突。
		                if (cachedAvailableSequence >= nextSequence)
		                {
		                		//从RingBuffer上获取事件。  
		                    event = ringBuffer.get(nextSequence);
		                    //委托给workHandler处理事件。  
		                    workHandler.onEvent(event);
		                    //设置事件处理完成标识。  
		                    processedSequence = true;
		                }
		                else
		                {
		                		//如果需要申请，通过序列栅栏来申请可用的序列。  
		                    cachedAvailableSequence = sequenceBarrier.waitFor(nextSequence);
		                }
		            }
		            catch (final TimeoutException e)
		            {
		                notifyTimeout(sequence.get());
		            }
		            catch (final AlertException ex)
		            {
		                if (!running.get())
		                {
		                    break;
		                }
		            }
		            catch (final Throwable ex)
		            {
		                // handle, mark as processed, unless the exception handler threw an exception
		                exceptionHandler.handleEventException(ex, nextSequence, event);
		                //如果异常处理器不抛出异常的话，就认为事件处理完毕，设置事件处理完成标识。
		                processedSequence = true;
		            }
		        }
		        //退出主循环后，如果workHandler实现了LifecycleAware，这里会对其进行一个关闭通知。  
		        notifyShutdown();
		        //设置当前处理器状态为停止。  
		        running.set(false);
		    }
		```
		
		- work模式必然是多消费者的，多消费者之间的处理的seq肯定需要是通一个，所以在构造WorkProcess的时候，需要传入一个workSequence,一般使用work模式的时候，都会使用WorkerPool进行管理。
		- 如存在多个WorkerPool，则RingBuffer中的事件是会被消费WorkerPoolNum次的，但在每个WorkerPool内，事件只会被一个WorkerProcessor消费。
	- NoOpEventProcessor
		