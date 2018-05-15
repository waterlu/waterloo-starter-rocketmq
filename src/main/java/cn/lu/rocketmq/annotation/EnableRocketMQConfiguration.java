package cn.lu.rocketmq.annotation;

import java.lang.annotation.*;

/**
 * 通过此注解开启RocketMQ功能
 *
 * @author lutiehua
 * @date 2017/7/11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EnableRocketMQConfiguration {
}