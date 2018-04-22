package con.erayt.yxc.disruptor.consumer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.lmax.disruptor.IgnoreExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;

import con.erayt.yxc.disruptor.base.TestEvent;
import con.erayt.yxc.disruptor.handler.MyWorkHandler;
import con.erayt.yxc.disruptor.producer.MyEventFactory;
import con.erayt.yxc.disruptor.translator.MyEventTranslator;

public class TwoWorkerPools {
	
	public static void main(String[] args) {
		RingBuffer<TestEvent> ring = RingBuffer.createSingleProducer(new MyEventFactory(), 4);
		
		MyWorkHandler[] handlers = new MyWorkHandler[4];
		for (int i = 0; i < 4; i++) {
			handlers[i] = new MyWorkHandler(i);
		}
		@SuppressWarnings("unchecked")
		WorkerPool<TestEvent> pool1 = new WorkerPool<TestEvent>(ring, ring.newBarrier(), new IgnoreExceptionHandler(), handlers[0],handlers[1]);
		@SuppressWarnings("unchecked")
		WorkerPool<TestEvent> pool2 = new WorkerPool<TestEvent>(ring, ring.newBarrier(), new IgnoreExceptionHandler(), handlers[2],handlers[3]);
		
		//通过增加gatingSequence来让生产者确定需要追踪的Seq
		ring.addGatingSequences(pool1.getWorkerSequences());
		ring.addGatingSequences(pool2.getWorkerSequences());
		Executor executor = Executors.newFixedThreadPool(4);  
		
		pool1.start(executor);
		pool2.start(executor);
		for (int i = 0; i < 10; i++) {
			ring.publishEvent(new MyEventTranslator());
			System.out.println(ring.get(i));
		}
		
//		RingBuffer<TestEvent> ring2 = RingBuffer.create(ProducerType.SINGLE, new MyEventFactory(), 16, new BlockingWaitStrategy());
//		for (int i = 0; i < 5; i++) {
//			ring2.publishEvent(new MyEventTranslator());
//			System.out.println(ring2.get(i));
//		}
	}
}
