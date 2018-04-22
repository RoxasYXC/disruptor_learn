package con.erayt.yxc.disruptor.consumer;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

import con.erayt.yxc.disruptor.base.TestEvent;
import con.erayt.yxc.disruptor.handler.AnotherEventHandler;
import con.erayt.yxc.disruptor.handler.MyEventHandler;
import con.erayt.yxc.disruptor.handler.PiplineEventHandler;
import con.erayt.yxc.disruptor.producer.MyEventFactory;
import con.erayt.yxc.disruptor.translator.MyEventTranslator;

   
   
/**      
 *       
 * @desc 描述  实现原理 根据Processor的执行先后顺序依次配置SequenceBarrier的追踪关系，让RingBuffer追踪最后一个Processor
 * @author yuxichen        
 * @version 1.0      
 * @created 2018年4月22日 下午6:10:47     
 */       
public class Pipline {
	
	public static void main(String[] args) {
		RingBuffer<TestEvent> ring = RingBuffer.createSingleProducer(new MyEventFactory(), 4);
		
		SequenceBarrier barrier1 = ring.newBarrier();
		BatchEventProcessor<TestEvent> processor = new BatchEventProcessor<TestEvent>(ring, barrier1, new MyEventHandler());
		
		SequenceBarrier barrier2 = ring.newBarrier(processor.getSequence());
		BatchEventProcessor<TestEvent> processor2 = new BatchEventProcessor<TestEvent>(ring, barrier2, new AnotherEventHandler());
		
		SequenceBarrier barrier3 = ring.newBarrier(processor2.getSequence());
		BatchEventProcessor<TestEvent> processor3 = new BatchEventProcessor<TestEvent>(ring, barrier3, new PiplineEventHandler());
		
		//通过增加gatingSequence来让生产者确定需要追踪的Seq
		ring.addGatingSequences(processor3.getSequence());
		
		new Thread(processor).start();
		new Thread(processor2).start();
		new Thread(processor3).start();
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
