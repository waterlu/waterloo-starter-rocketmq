package cn.lu.rocketmq.core;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * RocketMQ 消息消费者抽象基类
 *
 * @author lutiehua
 * @date 2017/7/11
 */
public abstract class AbstractMQConsumer<T extends MQMessage> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 消费组
     */
    private String groupName;

    /**
     * 是否可以重复消费（默认不可以）
     *
     */
    private boolean duplicated = false;

    public long getMaxHistoryTime() {
        return maxHistoryTime;
    }

    public void setMaxHistoryTime(long maxHistoryTime) {
        this.maxHistoryTime = maxHistoryTime;
    }

    /**
     * 最大相对历史消息时间（毫秒）
     */
    long maxHistoryTime;

    public AbstractMQConsumer() {

    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isDuplicated() {
        return duplicated;
    }

    public void setDuplicated(boolean duplicated) {
        this.duplicated = duplicated;
    }

    public long getBeginHistoryTime() {
        return beginHistoryTime;
    }

    public void setBeginHistoryTime(long beginHistoryTime) {
        this.beginHistoryTime = beginHistoryTime;
    }

    /**
     * 最早绝对历史消息时间（毫秒）
     */
    long beginHistoryTime;

    /**
     * 消息处理逻辑
     *
     * @param message
     * @return
     */
    public abstract boolean process(T message) throws MQException;

    /**
     * 收到消息
     *
     * @param messageExt
     * @param context
     * @return
     */
    public ConsumeConcurrentlyStatus consumeMessage(MessageExt messageExt, ConsumeConcurrentlyContext context) {
        T t = parseMessage(messageExt);

        String topic = messageExt.getTopic();
        String tag = messageExt.getTags();
        String group = getGroupName();
        String messageID = messageExt.getMsgId();
        String messageKey = messageExt.getKeys();
        long now = System.currentTimeMillis();
        long bornTime = messageExt.getBornTimestamp();
        long storeTime = messageExt.getStoreTimestamp();
        long commitLogOffset = messageExt.getCommitLogOffset();
        int queueId = messageExt.getQueueId();
        long queueOffset = messageExt.getQueueOffset();

        logger.info("收到消息：topic=[{}], tag=[{}], messageID=[{}], key=[{}], bornTime=[{}], storeTime=[{}], commitLogOffset=[{}], queueId=[{}], queueOffset=[{}]",
                topic, tag, messageID, messageKey, bornTime, storeTime, commitLogOffset, queueId, queueOffset);

        // 历史消息重复消费的判断（相对时间）
        if (t instanceof AbstractMQMessage) {
            // 自己存储了生成消息的时间，用自己的
            AbstractMQMessage mqMessage = (AbstractMQMessage)t;
            long createTime = mqMessage.getMessageCreateTime().getTime();
            long interval = now - createTime;
            if (interval > maxHistoryTime) {
                logger.warn("过期的相对历史消息：[主题:标签:消费组]=[{}:{}:{}], [key]=[{}], [createTime]=[{}], [interval]=[{}], [maxHistoryTime]=[{}]",
                        topic, tag, group, messageKey, createTime, interval, maxHistoryTime);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        } else {
            // 自己没存储消息生成时间，用MQ的
            long interval = now - bornTime;
            if (interval > maxHistoryTime) {
                logger.warn("过期的相对历史消息：[主题:标签:消费组]=[{}:{}:{}], [key]=[{}], [bornTime]=[{}], [interval]=[{}], [maxHistoryTime]=[{}]",
                        topic, tag, group, messageKey, bornTime, interval, maxHistoryTime);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        }

        // 历史消息重复消费的判断（绝对时间）
        if (beginHistoryTime > 0) {
            // 设置了绝对历史时间才判断
            if (t instanceof AbstractMQMessage) {
                // 自己存储了生成消息的时间，用自己的
                AbstractMQMessage mqMessage = (AbstractMQMessage)t;
                long createTime = mqMessage.getMessageCreateTime().getTime();
                if (createTime < beginHistoryTime) {
                    logger.warn("过期的绝对历史消息：[主题:标签:消费组]=[{}:{}:{}], [key]=[{}], [createTime]=[{}], [beginHistoryTime]=[{}]",
                            topic, tag, group, messageKey, createTime, beginHistoryTime);
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            } else {
                // 自己没存储消息生成时间，用MQ的
                if (bornTime < beginHistoryTime) {
                    logger.warn("过期的绝对历史消息：[主题:标签:消费组]=[{}:{}:{}], [key]=[{}], [bornTime]=[{}], [beginHistoryTime]=[{}]",
                            topic, tag, group, messageKey, bornTime, beginHistoryTime);
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            }
        }

        // 重复消息消费的判断
        String MESSAGE_CONSUME_HISTORY = "message_consume_history:%s:%s:%s";
        String redisKey = String.format(MESSAGE_CONSUME_HISTORY, topic, tag, group);
        if (!duplicated) {
            ZSetOperations<String, String> opsForZSet = redisTemplate.opsForZSet();
            // 尝试添加Key到ZSet中，排序值为当前时间
            long time = System.currentTimeMillis();
            if (!opsForZSet.add(redisKey, messageKey, time)) {
                // 添加失败，key已经存在，重复消费
                // 如果并发收到两条一模一样的消息，第二个zadd操作失败，丢弃第二个消息
                logger.warn("重复消费消息：[主题:标签:消费组]=[{}:{}:{}], [key]=[{}]", topic, tag, group, messageKey);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        }

        try {
            // 如果消息从AbstractMQMessage继承，添加bornTime和storeTime
            if (t instanceof AbstractMQMessage) {
                AbstractMQMessage mqMessage = (AbstractMQMessage)t;
                mqMessage.setMessageBornTime(new Date(bornTime));
                mqMessage.setMessageStoreTime(new Date(storeTime));
            }
            // 真正的消息处理逻辑
            if (!process(t)) {
                // 消费者没有处理完成，下次还需要处理，从Set中删除key
                if (!duplicated) {
                    removeKeyFromSet(redisKey, messageKey);
                }
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            if (!duplicated) {
                // 消费者处理出现异常，下次还需要处理，从Set中删除key
                removeKeyFromSet(redisKey, messageKey);
            }
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    /**
     * 从Set中清除已消费的Key
     *
     * @param redisKey
     * @param messageKey
     */
    private void removeKeyFromSet(String redisKey, String messageKey) {
        ZSetOperations<String, String> opsForZSet = redisTemplate.opsForZSet();
        if(opsForZSet.remove(redisKey, messageKey) > 0) {
            logger.info("remove {} from {}", messageKey, redisKey);
        } else {
            logger.warn("remove {} from {} failed,  return 0", messageKey, redisKey);
        }
    }

    /**
     * 反序列化解析消息
     *
     * @param message  消息体
     * @return
     */
    private T parseMessage(MessageExt message) {
        if (message == null || message.getBody() == null) {
            return null;
        }
        final Type type = this.getMessageType();
        if (type instanceof Class) {
            String jsonString = new String(message.getBody(), Charset.forName("UTF-8"));
            Object data = JSON.parseObject(jsonString, type);
            return (T) data;
        } else {
            return null;
        }
    }

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
            throw new RuntimeException("Unkown parameterized type.");
        }
    }
}
