package con.erayt.yxc.disruptor.producer;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.ProducerType;

import con.erayt.yxc.disruptor.base.TestEvent;
import con.erayt.yxc.disruptor.translator.MyEventTranslator;

public class SingleProducer {
	
	public static void main(String[] args) {
		RingBuffer<TestEvent> ring = RingBuffer.createSingleProducer(new MyEventFactory(), 16);
		for (int i = 0; i < 5; i++) {
			ring.publishEvent(new MyEventTranslator());
			System.out.println(ring.get(i));
		}
		
		RingBuffer<TestEvent> ring2 = RingBuffer.create(ProducerType.SINGLE, new MyEventFactory(), 16, new BlockingWaitStrategy());
		for (int i = 0; i < 5; i++) {
			ring2.publishEvent(new MyEventTranslator());
			System.out.println(ring2.get(i));
		}
	}
}
