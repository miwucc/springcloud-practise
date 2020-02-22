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
 *
 * 注意！！！！EurekaNotificationServerListUpdater在这个版本中不可用！！！这里只是以前按网上的思路写的例子，但经过实测并不能生效，原因如下：！
 * ribbon有两种更新服务列表的机制，一种默认的PollingServerListUpdater，这个是定时去更新discoveryClient的列表缓存过来，有延迟，一种是EurekaNotificationServerListUpdater，这种是在discoveryClient在列表更新后会抛个cacheRefreshedEvent出来，EurekaNotificationServerListUpdater监听这个事件，会立马更新ribbon LoadBalancer中的serverList。我就是把默认的改成后面这个了
 * 但是发现，实际在springcloud D.SR5这个版本里面，DiscoveryClient的实际实现CloudEurekaClient，把抛个cacheRefreshedEvent这个事件的方法onCacheRefreshed给重写了，里面换成了发心跳事件，然后EurekaNotificationServerListUpdater就一直触发不到，所以我服务注册列表里面变down了，ribbon里面就是不跟着变。
 * 所以这里必须注释掉，出发你自己按EurekaNotificationServerListUpdater的思路做定制修改，不然要影响ribbon对服务列表的发现。
 *
 */
@Configuration
public class ConsumerRibbonClientConfig {

    //EurekaNotificationServerListUpdater类有bug，先注释掉
//    @Bean
//    public ServerListUpdater ribbonServerListUpdater() {
//        return new EurekaNotificationServerListUpdater();
//    }

}
