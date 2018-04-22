package con.erayt.yxc.disruptor.handler;

import com.lmax.disruptor.EventHandler;

import con.erayt.yxc.disruptor.base.TestEvent;

public class AnotherEventHandler implements EventHandler<TestEvent> {

	@Override
	public void onEvent(TestEvent event, long sequence, boolean endOfBatch) throws Exception {
		System.out.println("HANDLE EVENT:" + event + ", SEQUENCE: "+ sequence + ",IS ENDOFBATCH:" + endOfBatch);
	}

}
