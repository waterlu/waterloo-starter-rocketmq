package cn.lu.rocketmq.core;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.nio.charset.Charset;

/**
 * RocketMQ 消息生产者的抽象基类
 *
 * @author lutiehua
 * @date 2017/7/11
 */
public abstract class AbstractMQProducer<T extends MQMessage> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 真正的消息发送者
     */
    private DefaultMQProducer producer;

    /**
     * 主题
     */
    private String topic;

    /**
     * 标签
     */
    private String tag;

    public AbstractMQProducer() {

    }

    @PreDestroy
    public void destroyProducer() {
        if (producer != null) {
            synchronized (AbstractMQProducer.class) {
                if (producer != null) {
                    producer.shutdown();
                }
            }
        }
    }

    /**
     * 发消息
     *
     * @param msgObj 消息内容
     * @return
     * @throws MQException
     */
    public boolean send(T msgObj) throws MQException {
        // 主题、标签、消息、主键都不能为空
        if(!checkMessage(msgObj)) {
            return false;
        }

        String key = msgObj.getKey();
        String jsonString = JSON.toJSONString(msgObj);
        byte [] body = jsonString.getBytes(Charset.forName(MQMessage.CHARSET));
        Message message = new Message(topic, tag, key, body);

        try {
            return sendMessage(message);
        } catch (Exception e) {
            throw new MQException("消息发送失败，topic=" + topic + ", e=" + e.getMessage());
        }
    }

    /**
     * 延时发送消息
     *
     * @param msgObj
     * @param delayTimeLevel
     * @return
     * @throws MQException
     */
    public boolean send(T msgObj, int delayTimeLevel) throws MQException {
        // 主题、标签、消息、主键都不能为空
        if(!checkMessage(msgObj)) {
            return false;
        }

        String key = msgObj.getKey();
        String jsonString = JSON.toJSONString(msgObj);
        byte [] body = jsonString.getBytes(Charset.forName(MQMessage.CHARSET));
        Message message = new Message(topic, tag, key, body);
        if (delayTimeLevel > 0 && delayTimeLevel <= 18) {
            message.setDelayTimeLevel(delayTimeLevel);
        }

        try {
            return sendMessage(message);
        } catch (Exception e) {
            throw new MQException("消息发送失败，topic :" + topic + ",e:" + e.getMessage());
        }
    }

    /**
     *
     * @param msgObj
     * @return
     */
    protected boolean checkMessage(T msgObj) throws MQException {
        // 主题、标签、消息内容都不能为空
        if (Strings.isNullOrEmpty(topic)) {
            throw new MQException("topic不能为空！");
        }

        if (Strings.isNullOrEmpty(tag)) {
            throw new MQException("tag不能为空！");
        }

        if (null == msgObj) {
            throw new MQException("msgObj不能为空！");
        }

        // 消息主键不能为空
        String key = msgObj.getKey();
        if (Strings.isNullOrEmpty(key)) {
            throw new MQException("消息key不能为空！");
        }

        return true;
    }

    /**
     *
     * @param message
     * @return
     */
    protected boolean sendMessage(Message message) throws Exception {
        SendResult sendResult = producer.send(message);
        SendStatus status = sendResult.getSendStatus();
        if (status.equals(SendStatus.SEND_OK)) {
            logger.info("发送消息：topic={}，tag={}, key={}", topic, tag, message.getKeys());
            return true;
        } else {
            logger.warn("发送消息失败：topic={}，tag={}, key={}, status={}", topic, tag, message.getKeys(), status.name());
            return false;
        }
    }

    public DefaultMQProducer getProducer() {
        return producer;
    }

    public void setProducer(DefaultMQProducer producer) {
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
