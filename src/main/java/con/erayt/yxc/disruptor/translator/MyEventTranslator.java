package con.erayt.yxc.disruptor.translator;

import com.lmax.disruptor.EventTranslator;

import con.erayt.yxc.disruptor.base.TestEvent;

public class MyEventTranslator implements EventTranslator<TestEvent>{

	@Override
	public void translateTo(TestEvent event, long sequence) {
		event.setId(sequence);
		event.setName("name" + sequence);
	}
	
}