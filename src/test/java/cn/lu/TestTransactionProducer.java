package cn.lu;

import cn.lu.rocketmq.annotation.RocketMQTransactionProducer;
import cn.lu.rocketmq.core.AbstractTransactionMQProducer;
import cn.lu.rocketmq.core.MQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lutiehua
 * @date 2017/7/13
 */
@RocketMQTransactionProducer(topic = "trans", tag = "none")
public class TestTransactionProducer extends AbstractTransactionMQProducer<TestMessage> {

    private final Logger logger = LoggerFactory.getLogger(TestTransactionProducer.class);

    @Override
    public boolean checkState(TestMessage msgObj) throws MQException {
        logger.info("checkState");
        return true;
    }
}