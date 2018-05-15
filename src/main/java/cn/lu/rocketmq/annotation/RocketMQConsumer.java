package cn.lu.rocketmq.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 消费者注解
 *
 * @author lutiehua
 * @date 2017/7/11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMQConsumer {

    /**
     * 消费组
     *
     * @return
     */
    String consumerGroup() default "";

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
    String tag() default "*";

    /**
     * 相同的Key是否可以重复消费（默认false）
     * true：可以重复消费
     * false：不可以重复消费
     *
     * @return
     */
    boolean duplicated() default false;

    /**
     * 最大历史消息时间（从消息生成时间到当前时间的间隔），单位为秒
     * 超过这个时间的历史消息，即使从MQ推送到Consumer，也会被丢弃掉
     * 默认最大历史消息时间为3天
     *
     * @return
     */
    long maxHistoryTime() default 3 * 24 * 60 * 60;

    /**
     * 消息生成的历史时间，在这个时间之前的消息将被丢弃
     * 时间格式"yyyy-MM-dd HH:mm:ss"，例如"2018-01-01 00:00:00"
     *
     * @return
     */
    String beginTime() default "";
}