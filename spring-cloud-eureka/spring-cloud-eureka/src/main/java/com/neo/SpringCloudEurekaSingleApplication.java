package com.neo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * 独立启动一个eureka服务
 */
@SpringBootApplication
//作为服务器启动
@EnableEurekaServer
public class SpringCloudEurekaSingleApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringCloudEurekaSingleApplication.class, args);
	}
}
