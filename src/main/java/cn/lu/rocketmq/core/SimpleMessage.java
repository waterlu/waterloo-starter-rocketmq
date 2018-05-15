package cn.lu.rocketmq.core;

/**
 * @author lutiehua
 * @date 2017/7/13
 */
public class SimpleMessage extends AbstractMQMessage {

    private String data;

    public SimpleMessage() {

    }

    public SimpleMessage(String data) {
        this.data = data;
    }

    @Override
    public String getKey() {
        return data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
