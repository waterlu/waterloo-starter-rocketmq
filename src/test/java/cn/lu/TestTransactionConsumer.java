package cn.lu;

import cn.lu.rocketmq.annotation.RocketMQConsumer;
import cn.lu.rocketmq.core.AbstractMQConsumer;
import cn.lu.rocketmq.core.MQException;

/**
 * @author lutiehua
 * @date 2017/7/13
 */
@RocketMQConsumer(topic = "trans", tag = "none", consumerGroup = "demo_trans")
public class TestTransactionConsumer extends AbstractMQConsumer<TestMessage> {

    @Override
    public boolean process(TestMessage message) throws MQException {
        return true;
    }
}