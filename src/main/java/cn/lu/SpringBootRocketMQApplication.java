package cn.lu;

import cn.lu.rocketmq.annotation.EnableRocketMQConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRocketMQConfiguration
public class SpringBootRocketMQApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootRocketMQApplication.class, args);
	}
}