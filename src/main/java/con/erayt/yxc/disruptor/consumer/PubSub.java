package con.erayt.yxc.disruptor.consumer;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;

import con.erayt.yxc.disruptor.base.TestEvent;
import con.erayt.yxc.disruptor.handler.AnotherEventHandler;
import con.erayt.yxc.disruptor.handler.MyEventHandler;
import con.erayt.yxc.disruptor.producer.MyEventFactory;
import con.erayt.yxc.disruptor.translator.MyEventTranslator;

public class PubSub {
	
	public static void main(String[] args) {
		RingBuffer<TestEvent> ring = RingBuffer.createSingleProducer(new MyEventFactory(), 4);
		
		BatchEventProcessor<TestEvent> processor = new BatchEventProcessor<TestEvent>(ring, ring.newBarrier(), new MyEventHandler());
		BatchEventProcessor<TestEvent> processor2 = new BatchEventProcessor<TestEvent>(ring, ring.newBarrier(), new AnotherEventHandler());
		
		//通过增加gatingSequence来让生产者确定需要追踪的Seq
		ring.addGatingSequences(processor.getSequence());
		ring.addGatingSequences(processor2.getSequence());
		
		new Thread(processor).start();
		new Thread(processor2).start();
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
