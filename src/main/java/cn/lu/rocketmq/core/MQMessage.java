package cn.lu.rocketmq.core;

/**
 * 消息
 *
 * @author lutiehua
 * @date 2017/7/13
 */
public interface MQMessage {

    /**
     * 消息按照UTF-8编码传输
     *
     */
    String CHARSET = "UTF-8";

    /**
     * 消息必须有Key
     *
     * @return
     */
    String getKey();
}