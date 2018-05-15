package cn.lu.rocketmq.config;

import cn.lu.rocketmq.annotation.RocketMQTransactionProducer;
import cn.lu.rocketmq.core.AbstractTransactionMQProducer;
import cn.lu.rocketmq.core.TransactionCheckListenerImpl;
import com.google.common.base.Strings;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author lutiehua
 * @date 2017/7/13
 */
@Configuration
@ConditionalOnBean(MQBaseAutoConfiguration.class)
public class TransactionMQProducerAutoConfiguration extends MQBaseAutoConfiguration {

    private TransactionMQProducer producer;

    private TransactionCheckListenerImpl transactionCheckListener;

    @PostConstruct
    public void init() throws Exception {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RocketMQTransactionProducer.class);
        if (beans.size() > 0) {
            // 有@RocketMQTransactionProducer注解才创建生产者
            if(producer == null) {
                String producerGroupName = mqProperties.getProducerTransactionGroupName();
                if(Strings.isNullOrEmpty(producerGroupName)) {
                    throw new RuntimeException("请在配置文件中指定Transaction Group Name！");
                }

                producer = new TransactionMQProducer(producerGroupName);
                producer.setCheckThreadPoolMinSize(2);
                producer.setCheckThreadPoolMaxSize(2);
                producer.setCheckRequestHoldMax(2000);
                producer.setNamesrvAddr(mqProperties.getNameServer());
                producer.setRetryTimesWhenSendFailed(mqProperties.getProducerRetry());
                producer.setSendMsgTimeout(mqProperties.getProducerTimeout());
                transactionCheckListener = new TransactionCheckListenerImpl();
                producer.setTransactionCheckListener(transactionCheckListener);
                producer.start();
            }

            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                publishProducer(entry.getKey(), entry.getValue());
            }
        }
    }

    private void publishProducer(String beanName, Object bean) throws Exception {
        if(!AbstractTransactionMQProducer.class.isAssignableFrom(bean.getClass())) {
            throw new RuntimeException(beanName + "未继承AbstractTransactionMQProducer");
        }

        RocketMQTransactionProducer mqProducer = applicationContext.findAnnotationOnBean(beanName, RocketMQTransactionProducer.class);
        AbstractTransactionMQProducer abstractMQProducer = (AbstractTransactionMQProducer) bean;
        abstractMQProducer.setProducer(producer);
        abstractMQProducer.setTopic(mqProducer.topic());
        abstractMQProducer.setTag(mqProducer.tag());
        transactionCheckListener.addListener(mqProducer.topic(), mqProducer.tag(), abstractMQProducer);
    }
}
