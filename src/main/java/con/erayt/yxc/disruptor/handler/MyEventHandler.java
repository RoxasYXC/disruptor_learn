package con.erayt.yxc.disruptor.handler;

import com.lmax.disruptor.EventHandler;

import con.erayt.yxc.disruptor.base.TestEvent;

public class MyEventHandler implements EventHandler<TestEvent> {

	@Override
	public void onEvent(TestEvent event, long sequence, boolean endOfBatch) throws Exception {
		Thread.sleep(500);
		System.out.println("handle event:" + event + ", sequence: "+ sequence + ",is endOfBatch:" + endOfBatch);
	}

}
