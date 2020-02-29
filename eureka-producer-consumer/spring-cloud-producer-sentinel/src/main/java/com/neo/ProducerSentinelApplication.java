package com.neo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
/**
 * EnableDiscoveryClient 注解把当前应用标识为一个springcloud服务
 * 1 向Eureka Server注册服务实例
 * 2 向Eureka Server服务续租
 * 3 当服务政策关闭时，向Eureka Server发送租约取消
 * 4 查询拉取Eureka Server中的服务实例列表
 * 以上需要配置一个注册中心地址 eureka.client.serviceUrl.defaultZone
 * 具体请关注EurekaDiscoveryClient类，这个类是spring cloud最大的两个发现服务接口DiscoveryClient和LookUpService接口的eureka实现类
 *
 *  EndPointUtils.getServiceUrlsMapFromConfig 方法
 *
 *  eureka其实可以区分region和zone，getServiceUrlsMapFromConfig方法就是先查出对应的region和zone，defalut zone就是
 *  eureka.client.serviceUrl.defaultZone这个配置,zone可以有多个地址,从而获获取到对应的serviceUrls
 *  ribbon在做服务负载均衡调用的时候，会优先调用与客户端同zone的服务实例
 *
 *  DiscoveryClient.initScheduledTasks 方法初始化了服务注册，服务续约和服务列表获取3个定时任务
 *
 *  client的所有client properties配置可以参考类 EurekaClientConfigBean
 *
 */
public class ProducerSentinelApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(ProducerSentinelApplication.class, args);

//		模拟30s后正常关闭服务，这个时候会发unregister和cancel事件给eurekaServer
//		Thread.sleep(1000L*30L);
//
//		System.exit(1);
//		System.out.println("exit!!!!!");

	}
}
