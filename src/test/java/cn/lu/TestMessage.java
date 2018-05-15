package cn.lu;

import cn.lu.rocketmq.core.AbstractMQMessage;
import java.util.Date;

/**
 * @author lutiehua
 * @date 2017/7/11
 */
public class TestMessage extends AbstractMQMessage {

    private String key;

    private String text;

    public TestMessage() {
        Date now = new Date();
        long time = now.getTime();
        key = Long.toHexString(time);
        text = now.toString();
    }

    @Override
    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "TestMessage{" +
                "key='" + key + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
