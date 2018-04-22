package con.erayt.yxc.disruptor.producer;

import com.lmax.disruptor.EventFactory;

import con.erayt.yxc.disruptor.base.TestEvent;

public class MyEventFactory implements EventFactory<TestEvent>{

	@Override
	public TestEvent newInstance() {
		return new TestEvent();
	}
	
}
