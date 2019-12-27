package com.neo.controller;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * 定义loadblance的restTemplate，用于获取微服务
     * localbalancer的自动配置类在=LoadBalancerAutoConfiguration
     * LoadBalancerAutoConfiguration中是对注解了@LaodBalnaced的RestTemplate加入一个拦截器LoadBalanceIntercepter，
     * 在拦截器中使用LoadBanlanceClient进行负载均衡处理，而这个client的实现就叫Nextflix Ribbon  ：RibbonLoadBalancerClient
     * RibbonClientConfiguration这个类对ribbon客户端进行了默认配置 中默认使用了ZoneAwareLoadBalancer，继承的Iloadbalancer接口基类BaseLoadBalancer(定义了基础Iping，Irule,serverList等)的子类DynamicServerListLoadBalancer基类(扩展了运行期间serverList更新功能，ServerList<T>接口，过滤选择功能)
     *
     * 流程如下：
     * 请求->RestTemplate->LoadBalanceIntercepter->LoadBanlanceClient(RibbonLoadBalancerClient)->clientFactory根据ServerId(服务名)获取对应loadBlancer
     * LoadBanlanceClient 中有一个引用clientFactory，里面的instance维护就算作我们这里叫的ribbonClient，这个里面负责每一个服务对应的loadBlancer实例，RibbonloadbalanceContext信息
     * https://blog.csdn.net/qq_27529917/article/details/80981109 （这个文章讲了关键的SpringClientFactory创建过程，要仔细看看）
     * https://www.codercto.com/a/33356.html
     * @return
     */
    @Bean
    @LoadBalanced
    RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
