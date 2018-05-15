package cn.lu.rocketmq.config;

import cn.lu.rocketmq.annotation.RocketMQConsumer;
import cn.lu.rocketmq.core.AbstractMQConsumer;
import com.google.common.base.Strings;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消费者自动配置
 *
 * @author lutiehua
 * @date 2017/7/11
 */
@Configuration
@ConditionalOnBean(MQBaseAutoConfiguration.class)
public class MQConsumerAutoConfiguration extends MQBaseAutoConfiguration {

    protected final Logger logger = LoggerFactory.getLogger(MQConsumerAutoConfiguration.class);

    private Map<String, DefaultMQPushConsumer> consumerMap = new ConcurrentHashMap<>();

    private Map<String, AbstractMQConsumer> processorMap = new ConcurrentHashMap<>();

    private Map<String, Group> groupMap = new ConcurrentHashMap<>();

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 消费组
     */
    public class Group {

        public String name;

        public Map<String, Topic> topics = new HashMap<>();

    }

    /**
     * 主题
     */
    public class Topic {

        public String name;

        public Map<String, String> tags = new HashMap<>();

    }

    @PostConstruct
    public void init() throws Exception {
        // 扫描并解析@RocketMQConsumer注解
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RocketMQConsumer.class);
        if (beans.size() > 0) {
            String consumerGroup = mqProperties.getConsumerGroupName();
            DefaultMQPushConsumer consumer = createConsumer(consumerGroup);
            consumerMap.put(consumerGroup, consumer);

            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                publishConsumer(entry.getKey(), entry.getValue());
            }

            // 处理标签
            for (Group group : groupMap.values()) {
                DefaultMQPushConsumer mqPushConsumer = consumerMap.get(group.name);
                for (Topic topic : group.topics.values()) {
                    StringBuffer buffer = new StringBuffer();
                    int count = 0;
                    for (String tag : topic.tags.values()) {
                        if (!Strings.isNullOrEmpty(tag)) {
                            if (count > 0) {
                                buffer.append("||");
                            }
                            buffer.append(tag);
                            count ++;
                        }
                    }

                    String tags = buffer.toString();
                    logger.info("subscribe(\"{}\", \"{}\") for group [{}]", topic.name, tags, group.name);
                    mqPushConsumer.subscribe(topic.name, tags);
                }
            }

            for (DefaultMQPushConsumer mqPushConsumer : consumerMap.values()) {
                mqPushConsumer.start();
            }
        }
    }

    private void publishConsumer(String beanName, final Object bean) throws Exception {
        RocketMQConsumer mqConsumer = applicationContext.findAnnotationOnBean(beanName, RocketMQConsumer.class);
        String consumerGroupName = mqConsumer.consumerGroup();
        String topic = mqConsumer.topic();
        String tag = mqConsumer.tag();
        boolean duplicated = mqConsumer.duplicated();
        // 应用设置时间单位是秒，MQ设置时间单位是毫秒
        long maxHistoryTime = mqConsumer.maxHistoryTime() * 1000;
        String beginTime = mqConsumer.beginTime();

        // 设置consumer，订阅主题
        DefaultMQPushConsumer consumer = null;

        // 没有指定消费组，使用默认的消费组
        if (Strings.isNullOrEmpty(consumerGroupName)) {
            consumerGroupName = mqProperties.getConsumerGroupName();
        }

        // 判断消费组是否存在
        if (consumerMap.containsKey(consumerGroupName)) {
            // 已经存在，复用consumer
            consumer = consumerMap.get(consumerGroupName);
        } else {
            // 创建新的consumer
            consumer = createConsumer(consumerGroupName);
            consumerMap.put(consumerGroupName, consumer);
        }

        Group group = null;
        if (groupMap.containsKey(consumerGroupName)) {
            group = groupMap.get(consumerGroupName);
        } else {
            group = new Group();
            group.name = consumerGroupName;
            groupMap.put(group.name, group);
        }

        Topic topicObj = null;
        for (Topic oldTopic : group.topics.values()) {
            if (oldTopic.name.equalsIgnoreCase(topic)) {
                topicObj = oldTopic;
                break;
            }
        }

        if (null == topicObj) {
            topicObj = new Topic();
            topicObj.name = topic;
            group.topics.put(topicObj.name, topicObj);
        }

        topicObj.tags.put(tag, tag);

        // 设置主题的处理方法
        if(!AbstractMQConsumer.class.isAssignableFrom(bean.getClass())) {
            throw new RuntimeException(bean.getClass().getName() +
                    " - consumer is not a subclass of class AbstractMQConsumer");
        }
        AbstractMQConsumer abstractMQConsumer = (AbstractMQConsumer) bean;
        abstractMQConsumer.setGroupName(consumerGroupName);
        abstractMQConsumer.setDuplicated(duplicated);
        abstractMQConsumer.setMaxHistoryTime(maxHistoryTime);
        if (!Strings.isNullOrEmpty(beginTime)) {
            try {
                Date historyDate = simpleDateFormat.parse(beginTime);
                abstractMQConsumer.setBeginHistoryTime(historyDate.getTime());
            } catch (Exception e) {
                logger.error(e.getMessage());
                abstractMQConsumer.setBeginHistoryTime(0);
            }
        } else {
            abstractMQConsumer.setBeginHistoryTime(0);
        }
        String processorKey = String.format("%s:%s:%s", topic, tag, consumerGroupName);
        if (processorMap.containsKey(processorKey)) {
            throw new RuntimeException(bean.getClass().getName() + " - find same [topic:tag:consumerGroup]=" + processorKey);
        }
        processorMap.put(processorKey, abstractMQConsumer);
    }

    /**
     * 创建新的Consumer
     *
     * @param consumerGroupName 消费组名称
     * @return
     */
    private DefaultMQPushConsumer createConsumer(String consumerGroupName) {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroupName);
        consumer.setNamesrvAddr(mqProperties.getNameServer());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                if (list.size() != 1) {
                    logger.error("消息大小异常 list.size()={}", list.size());
                } else {
                    // consumeMessageBatchMaxSize默认值为1，我们没有修改，所以List里面只能有一个元素
                    MessageExt messageExt = list.get(0);
                    String topic = messageExt.getTopic();
                    String tag = messageExt.getTags();
                    String processorKey = String.format("%s:%s:%s", topic, tag, consumerGroupName);
                    if (processorMap.containsKey(processorKey)) {
                        AbstractMQConsumer abstractMQConsumer = processorMap.get(processorKey);
                        ConsumeConcurrentlyStatus status = abstractMQConsumer.consumeMessage(messageExt, consumeConcurrentlyContext);
                        if (status == ConsumeConcurrentlyStatus.RECONSUME_LATER) {
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    } else {
                        // 正常情况不应该走到这里
                        logger.error("没有找到消费处理者[{}]", processorKey);
                    }
                }

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        return consumer;
    }
}
