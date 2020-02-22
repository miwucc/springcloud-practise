package com.neo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ConsumerApplication {

	/**
	 * 服务是否已经启动
	 */
	public volatile static boolean isStart = false;
	/**
	 * 服务启动的时间
	 */
	public volatile static long startTime = 0;
	/**
	 * 服务是否处于准备关闭的状态，这个时候一些定时任务就应该及时退出了
	 */
	public volatile static boolean isPreStop = false;

	public static void main(String[] args) {
		SpringApplication.run(ConsumerApplication.class, args);
//		logger.info("服务启动完毕");
		startTime = System.currentTimeMillis();
		isStart = true;
	}

	@PostConstruct
	public void preload(){
//		logger.info("预加载完毕");
	}

	//这里只是随便写个方法距离，只是说提供一个就绪接口给k8s做检查
	public boolean ifReady() {
		long timeCur = System.currentTimeMillis();
		if (isStart && !isPreStop && timeCur - startTime > 16000) {
			return true;//success
		} else {
			//throw Exception在外层做异常处理并返回500的http code
			return false;
		}
	}



}
