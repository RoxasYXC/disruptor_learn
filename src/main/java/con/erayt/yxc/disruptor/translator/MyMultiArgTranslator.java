package con.erayt.yxc.disruptor.translator;

import com.lmax.disruptor.EventTranslatorVararg;

import con.erayt.yxc.disruptor.base.TestEvent;

public class MyMultiArgTranslator implements EventTranslatorVararg<TestEvent>{

	@Override
	public void translateTo(TestEvent event, long sequence, Object... args) {
		event.setId((long) args[0]);
	}

}
