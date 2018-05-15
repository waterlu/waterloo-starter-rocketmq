package cn.lu.rocketmq.config;

import cn.lu.rocketmq.annotation.EnableRocketMQConfiguration;
import cn.lu.rocketmq.core.AbstractMQConsumer;
import cn.lu.rocketmq.core.AbstractMQProducer;
import cn.lu.rocketmq.core.AbstractTransactionMQProducer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置入口
 *
 * @author lutiehua
 * @date 2017/7/13
 */
@Configuration
@ConditionalOnBean(annotation = EnableRocketMQConfiguration.class)
@AutoConfigureAfter({AbstractMQProducer.class, AbstractMQConsumer.class, AbstractTransactionMQProducer.class})
@EnableConfigurationProperties(MQProperties.class)
public class MQBaseAutoConfiguration implements ApplicationContextAware {

    @Autowired
    protected MQProperties mqProperties;

    protected ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}