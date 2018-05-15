package cn.lu;

import cn.lu.rocketmq.annotation.RocketMQProducer;
import cn.lu.rocketmq.core.AbstractMQProducer;

/**
 * @author lutiehua
 * @date 2017/7/11
 */
@RocketMQProducer(topic = "test", tag = "time")
public class TestProducer extends AbstractMQProducer<TestMessage> {

}