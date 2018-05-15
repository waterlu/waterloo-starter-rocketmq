package cn.lu.rocketmq.config;

import cn.lu.rocketmq.annotation.RocketMQProducer;
import cn.lu.rocketmq.core.AbstractMQProducer;
import com.google.common.base.Strings;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 配置生产者
 *
 * @author lutiehua
 * @date 2017/7/11
 */
@Configuration
@ConditionalOnBean(MQBaseAutoConfiguration.class)
public class MQProducerAutoConfiguration extends MQBaseAutoConfiguration {

    /**
     * 生产者实例
     */
    private DefaultMQProducer producer;

    @PostConstruct
    public void init() throws Exception {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RocketMQProducer.class);
        if (beans.size() > 0) {
            // 有@RocketMQProducer注解才创建生产者
            if(producer == null) {
                if(Strings.isNullOrEmpty(mqProperties.getProducerGroupName())) {
                    throw new RuntimeException("请在配置文件中指定消息发送方group！");
                }
                producer = new DefaultMQProducer(mqProperties.getProducerGroupName());
                producer.setNamesrvAddr(mqProperties.getNameServer());
                producer.setRetryTimesWhenSendFailed(mqProperties.getProducerRetry());
                producer.setRetryTimesWhenSendAsyncFailed(mqProperties.getProducerRetry());
                producer.setSendMsgTimeout(mqProperties.getProducerTimeout());
                producer.setCompressMsgBodyOverHowmuch(4096);
                producer.setMaxMessageSize(4194304);
                producer.setRetryAnotherBrokerWhenNotStoreOK(false);
                producer.setVipChannelEnabled(false);
                producer.start();
            }

            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                publishProducer(entry.getKey(), entry.getValue());
            }
        }
    }

    private void publishProducer(String beanName, Object bean) throws Exception {
        if(!AbstractMQProducer.class.isAssignableFrom(bean.getClass())) {
            throw new RuntimeException(beanName + " - producer未继承AbstractMQProducer");
        }

        RocketMQProducer mqProducer = applicationContext.findAnnotationOnBean(beanName, RocketMQProducer.class);
        AbstractMQProducer abstractMQProducer = (AbstractMQProducer) bean;
        abstractMQProducer.setProducer(producer);
        abstractMQProducer.setTopic(mqProducer.topic());
        abstractMQProducer.setTag(mqProducer.tag());
    }

}
