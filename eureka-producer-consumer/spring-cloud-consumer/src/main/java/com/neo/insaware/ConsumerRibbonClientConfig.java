package com.neo.insaware;

import com.netflix.loadbalancer.ServerListUpdater;
import com.netflix.niws.loadbalancer.EurekaNotificationServerListUpdater;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description 配置这个的目的是为了在收到DiscovertClient的列表更新时间后，告知ribbon缓存也进行同步更新
 * 只能在这儿配置，properties里面配置了不生效
 * https://blog.csdn.net/u010457081/article/details/88627926Polling
 *  DynamicServerListLoadBalancer中定义了一个ServerListUpdater.UpdateAction类型的服务更新器，Spring Cloud提供了两种服务更新策略：一种是PollingServerListUpdater，表示定时更新；另一种是EurekaNotificationServerListUpdater表示由Eureka的事件监听来驱动服务列表的更新操作，默认的实现策略是第一种，即定时更新，定时的方式很简单，创建Runnable，调用DynamicServerListLoadBalancer中updateAction对象的doUpdate方法，Runnable延迟启动时间为1秒，重复周期为30秒。
 * @Author xl
 * @Date 2019/7/22
 */
@Configuration
public class ConsumerRibbonClientConfig {

    @Bean
    public ServerListUpdater ribbonServerListUpdater() {
        return new EurekaNotificationServerListUpdater();
    }

}
