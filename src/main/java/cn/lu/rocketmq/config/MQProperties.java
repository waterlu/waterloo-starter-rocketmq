package cn.lu.rocketmq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lutiehua
 * @date 2017/7/11
 */
@ConfigurationProperties(prefix = "rocketMq")
public class MQProperties {

    /**
     * Name Server 地址
     */
    private String nameServer;

    /**
     * 生产者默认组
     */
    private String producerGroupName = "producer";

    /**
     * 事务生产者默认组
     */
    private String producerTransactionGroupName = "t_producer";

    /**
     * 发送重试次数
     */
    private Integer producerRetry = 3;

    /**
     * 发送超时时间
     */
    private Integer producerTimeout = 10000;

    /**
     * 消费者默认组
     */
    private String consumerGroupName = "consumer";

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getProducerGroupName() {
        return producerGroupName;
    }

    public void setProducerGroupName(String producerGroupName) {
        this.producerGroupName = producerGroupName;
    }

    public String getProducerTransactionGroupName() {
        return producerTransactionGroupName;
    }

    public void setProducerTransactionGroupName(String producerTransactionGroupName) {
        this.producerTransactionGroupName = producerTransactionGroupName;
    }

    public Integer getProducerRetry() {
        return producerRetry;
    }

    public void setProducerRetry(Integer producerRetry) {
        this.producerRetry = producerRetry;
    }

    public Integer getProducerTimeout() {
        return producerTimeout;
    }

    public void setProducerTimeout(Integer producerTimeout) {
        this.producerTimeout = producerTimeout;
    }

    public String getConsumerGroupName() {
        return consumerGroupName;
    }

    public void setConsumerGroupName(String consumerGroupName) {
        this.consumerGroupName = consumerGroupName;
    }
}
