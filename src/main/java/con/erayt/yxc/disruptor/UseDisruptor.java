package con.erayt.yxc.disruptor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.lmax.disruptor.dsl.Disruptor;

import con.erayt.yxc.disruptor.base.TestEvent;
import con.erayt.yxc.disruptor.handler.AnotherEventHandler;
import con.erayt.yxc.disruptor.handler.MyEventHandler;
import con.erayt.yxc.disruptor.producer.MyEventFactory;
import con.erayt.yxc.disruptor.translator.MyEventTranslator;

   
/**      
 *       
 * @desc 描述 使用封装的api直接使用   
 * @author yuxichen        
 * @version 1.0      
 * @created 2018年4月22日 下午9:09:16     
 */       
public class UseDisruptor {
	public static void main(String[] args) {
		Executor executor = Executors.newFixedThreadPool(4);  
		@SuppressWarnings("deprecation")
		Disruptor<TestEvent> disruptor = new Disruptor<TestEvent>(new MyEventFactory(), 4, executor);
		disruptor.handleEventsWith(new MyEventHandler()).then(new AnotherEventHandler());
		disruptor.start();
		for (int i = 0; i < 10; i++) {
			disruptor.publishEvent(new MyEventTranslator());
		}
		disruptor.shutdown();
	}
}
