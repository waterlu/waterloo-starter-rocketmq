package cn.lu;

import org.apache.rocketmq.client.producer.LocalTransactionExecuter;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootRocketMQApplicationTests {

	private final Logger logger = LoggerFactory.getLogger(SpringBootRocketMQApplicationTests.class);

	@Autowired
	private TestProducer testProducer;

	@Autowired
	private TestTransactionProducer testTransProducer;

	public void contextLoads() {

	}

	@Test
	public void testProducer() {
		try {
			TestMessage message = new TestMessage();
			Assert.assertTrue(testProducer.send(message));
			Thread.sleep(3000);
			logger.info("test done");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testTransProducer() {
		try {
			TestMessage message = new TestMessage();
			TransactionSendResult result = testTransProducer.send(message, new LocalTransactionExecuter() {
				@Override
				public LocalTransactionState executeLocalTransactionBranch(Message msg, Object arg) {
					logger.info("executeLocalTransactionBranch");
					return LocalTransactionState.UNKNOW;
				}
			}, null);
			Thread.sleep(3000);
			logger.info(result.getLocalTransactionState().name());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
