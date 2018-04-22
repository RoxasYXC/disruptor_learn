package con.erayt.yxc.disruptor.handler;

import com.lmax.disruptor.WorkHandler;

import con.erayt.yxc.disruptor.base.TestEvent;

public class MyWorkHandler implements WorkHandler<TestEvent> {

	private int workerId;
	
	public MyWorkHandler(int workerId) {
		super();
		this.workerId = workerId;
	}

	@Override
	public void onEvent(TestEvent event) throws Exception {
		System.out.println("worker "+workerId+" handle event:" + event);
	}

}
