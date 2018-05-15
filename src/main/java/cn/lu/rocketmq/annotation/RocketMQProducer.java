package cn.lu.rocketmq.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 生产者注解
 *
 * @author lutiehua
 * @date 2017/7/11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMQProducer {

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