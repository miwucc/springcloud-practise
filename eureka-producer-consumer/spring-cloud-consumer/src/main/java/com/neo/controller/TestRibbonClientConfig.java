package com.neo.controller;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RoundRobinRule;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用来测试自定义的Ribbon配置
 * @RibbonClient和@Configuration注解是必须要的
 * 这个配置将只对spring-cloud-producer 服务生效，这个等效于在properties文件中配置 spring-cloud-producer.ribbon.NFLoadBalancerRuleClassName = com.neo.controller.TestRibbonClientConfig
 * 这个是否生效可以在 NameConextedFactory.createContext(String name)方法中打断点查看
 * 注意 spring-cloud-producer这个大小写敏感，一定要和服务端配置一样
 * @RibbonClient中value或者name必须至少配置一个，否则会报错,value和name等价，没有啥区别，参看RibbonClientConfigurationRegistrar.registerBeanDefinitions方法
 */


//@RibbonClients 可用来定义多和服务，但是没有name，只能配置value
@RibbonClient(name = "spring-cloud-producer", configuration = {TestRibbonClientConfig.class})
@Configuration
public class TestRibbonClientConfig {

    /**
     * 自定义负载均衡准则
     * 这个配置bean会优先于properties配置文件，这里也可以用自己的自定义负载均衡策略，RibbonClient的name可以用于配置proiperties中的
     * <clientName>.ribbon.NFLoadBalancerRuleClassName
     * 这里为了测试properties中的NFLoadBalancerRuleClassName配置，这里先关掉
     * @return
     */
//    @Bean
//    public IRule ribbonRule() {
//        return new RoundRobinRule();
//    }


}
