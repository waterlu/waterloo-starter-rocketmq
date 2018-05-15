package cn.lu;

import cn.lu.rocketmq.annotation.RocketMQConsumer;
import cn.lu.rocketmq.core.AbstractMQConsumer;
import cn.lu.rocketmq.core.MQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lutiehua
 * @date 2017/7/11
 */
@RocketMQConsumer(topic = "test", tag = "time", consumerGroup = "demo")
public class TestConsumer extends AbstractMQConsumer<TestMessage> {

    private final Logger logger = LoggerFactory.getLogger(TestConsumer.class);

    @Override
    public boolean process(TestMessage message) throws MQException {
        logger.info("receive: " + message.toString());
        return true;
    }
}