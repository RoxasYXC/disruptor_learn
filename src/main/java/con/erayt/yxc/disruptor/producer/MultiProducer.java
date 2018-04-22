package con.erayt.yxc.disruptor.producer;

import java.util.concurrent.CountDownLatch;

import com.lmax.disruptor.RingBuffer;

import con.erayt.yxc.disruptor.base.TestEvent;
import con.erayt.yxc.disruptor.translator.MyMultiArgTranslator;

public class MultiProducer {
	public static void main(String[] args) {
		final RingBuffer<TestEvent> ring = RingBuffer.createMultiProducer(new MyEventFactory(), 256);
		final CountDownLatch latch = new CountDownLatch(100);
		for (int i = 0; i < 100; i++) {
			final long index = i;
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					try{
						ring.publishEvent(new MyMultiArgTranslator(), index);
					}finally{
						latch.countDown();
					}
				}
			}).start();
		}
		try {
			latch.await();
			for (int j = 0; j < 100; j++) {
				System.out.println(ring.get(j));
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
