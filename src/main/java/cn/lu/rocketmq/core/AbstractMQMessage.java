package cn.lu.rocketmq.core;

import java.util.Date;
import java.util.UUID;

/**
 * @author lutiehua
 * @date 2018/1/9
 */
public abstract class AbstractMQMessage implements MQMessage {

    /**
     * 消息生成的时间（MQ的）
     *
     */
    private Date messageBornTime;

    /**
     * 消息落盘的时间（MQ的）
     */
    private Date messageStoreTime;

    /**
     * 创建消息的系统时间
     */
    private final Date messageCreateTime;

    /**
     * 消息必须有Key
     *
     * @return
     */
    @Override
    public String getKey() {
        // 提供UUID作为默认的Key
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public AbstractMQMessage() {
        messageCreateTime = new Date();
    }

    public Date getMessageCreateTime() {
        return messageCreateTime;
    }

    public Date getMessageStoreTime() {
        return messageStoreTime;
    }

    public void setMessageStoreTime(Date messageStoreTime) {
        this.messageStoreTime = messageStoreTime;
    }

    public Date getMessageBornTime() {
        return messageBornTime;
    }

    public void setMessageBornTime(Date messageBornTime) {
        this.messageBornTime = messageBornTime;
    }
}
