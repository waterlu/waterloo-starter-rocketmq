package cn.lu.rocketmq.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 事务消息生产者注解
 *
 * @author lutiehua
 * @date 2017/7/13
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMQTransactionProducer {

    /**
     * 主题
     *
     * @return
     */
    String topic();

    /**
     * 标签
     *
     * @return
     */
    String tag() default "";
}