package cn.lu.rocketmq.core;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionCheckListener;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lutiehua
 * @date 2017/7/13
 */
public class TransactionCheckListenerImpl implements TransactionCheckListener {

    private Map<String, AbstractTransactionMQProducer> producerMap = new ConcurrentHashMap();

    public void addListener(String topic, String tag, AbstractTransactionMQProducer producer) {
        String key = String.format("%s:%s", topic, tag);
        producerMap.put(key, producer);
    }

    @Override
    public LocalTransactionState checkLocalTransactionState(MessageExt messageExt) {
        String topic = messageExt.getTopic();
        String tag = messageExt.getTags();
        String key = String.format("%s:%s", topic, tag);

        AbstractTransactionMQProducer producer = producerMap.get(key);
        if (null == producer) {
            return LocalTransactionState.UNKNOW;
        }

        return producer.checkLocalTransactionState(messageExt);
    }
}