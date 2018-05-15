package cn.lu.rocketmq.core;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import org.apache.rocketmq.client.producer.LocalTransactionExecuter;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * RocketMQ 事务消息生产者的抽象基类
 *
 * @author lutiehua
 * @date 2017/7/13
 */
public abstract class AbstractTransactionMQProducer<T extends MQMessage> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 事务消息生产者
     */
    private TransactionMQProducer producer;

    /**
     * 主题
     */
    private String topic;

    /**
     * 标签
     */
    private String tag;

    public AbstractTransactionMQProducer() {

    }

    @PreDestroy
    public void destroyProducer() {
        if (producer != null) {
            synchronized (AbstractTransactionMQProducer.class) {
                if (producer != null) {
                    producer.shutdown();
                }
            }
        }
    }

    /**
     * 发送事务消息
     *
     * @param msgObj
     * @param transExecuter
     * @param arg
     * @return
     * @throws MQException
     */
    public TransactionSendResult send(T msgObj, LocalTransactionExecuter transExecuter, Object arg) throws MQException {
        try {
            // 主题、标签、消息内容都不能为空
            if(Strings.isNullOrEmpty(topic) || Strings.isNullOrEmpty(tag) || null == msgObj) {
                return null;
            }

            // 消息主键不能为空
            String key = msgObj.getKey();
            if (Strings.isNullOrEmpty(key)) {
                return null;
            }

            String jsonString = JSON.toJSONString(msgObj);
            byte [] body = jsonString.getBytes(Charset.forName(MQMessage.CHARSET));
            Message message = new Message(topic, tag, key, body);
            TransactionSendResult sendResult = producer.sendMessageInTransaction(message, transExecuter, arg);
            if (sendResult.getLocalTransactionState().equals(LocalTransactionState.COMMIT_MESSAGE)) {
                logger.info("发送事务消息：topic={}，tag={}, key={}, body={}", topic, tag, key, jsonString);
            } else if (sendResult.getLocalTransactionState().equals(LocalTransactionState.ROLLBACK_MESSAGE)) {
                logger.info("撤回事务消息：topic={}，tag={}, key={}, body={}", topic, tag, key, jsonString);
            } else {
                logger.info("待确认事务消息：topic={}，tag={}, key={}, body={}, status={}", topic, tag, key, jsonString,
                        sendResult.getLocalTransactionState().name());
            }

            return sendResult;
        } catch (Exception e) {
            throw new MQException("事务消息发送失败，topic:" + topic + ", tag:" + tag + ", e:" + e.getMessage());
        }
    }

    /**
     * 检查事务消息状态
     *
     * @param message
     * @return
     */
    public LocalTransactionState checkLocalTransactionState(MessageExt message) {
        if (message == null || message.getBody() == null) {
            return LocalTransactionState.UNKNOW;
        }

        String jsonString = new String(message.getBody(), Charset.forName("UTF-8"));
        final Type type = this.getMessageType();
        T msgObj = JSON.parseObject(jsonString, type);
        logger.info("检查事务消息：topic={}，tag={}, key={}", topic, tag, msgObj.getKey());
        try {
            if (checkState(msgObj)) {
                return LocalTransactionState.COMMIT_MESSAGE;
            } else {
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return LocalTransactionState.UNKNOW;
        }
    }

    /**
     * 检查逻辑
     *
     * @param msgObj
     * @return
     */
    public abstract boolean checkState(T msgObj) throws MQException;

    /**
     * 解析消息类型
     *
     * @return
     */
    private Type getMessageType() {
        Type superType = this.getClass().getGenericSuperclass();
        if (superType instanceof ParameterizedType) {
            return ((ParameterizedType) superType).getActualTypeArguments()[0];
        } else {
            throw new RuntimeException("Unknown parameterized type.");
        }
    }

    public TransactionMQProducer getProducer() {
        return producer;
    }

    public void setProducer(TransactionMQProducer producer) {
        this.producer = producer;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
