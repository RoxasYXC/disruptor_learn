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

public class TwoWorkers {
	
	public static void main(String[] args) {
		RingBuffer<TestEvent> ring = RingBuffer.createSingleProducer(new MyEventFactory(), 4);
		
		MyWorkHandler[] handlers = new MyWorkHandler[2];
		for (int i = 0; i < 2; i++) {
			handlers[i] = new MyWorkHandler(i);
		}
		WorkerPool<TestEvent> pool = new WorkerPool<TestEvent>(ring, ring.newBarrier(), new IgnoreExceptionHandler(), handlers);
		
		//通过增加gatingSequence来让生产者确定需要追踪的Seq
		ring.addGatingSequences(pool.getWorkerSequences());
		Executor executor = Executors.newFixedThreadPool(4);  
		
		pool.start(executor);
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
