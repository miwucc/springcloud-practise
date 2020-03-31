//package com.neo.insaware;
//
//import com.netflix.loadbalancer.ServerListUpdater;
//import com.netflix.niws.loadbalancer.EurekaNotificationServerListUpdater;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * @Description 配置这个的目的是为了在收到DiscovertClient的列表更新时间后，告知ribbon缓存也进行同步更新
// * 只能在这儿配置，properties里面配置了不生效
// * https://blog.csdn.net/u010457081/article/details/88627926
// * @Author xl
// * @Date 2019/7/22
// */
//@Configuration
//public class ConsumerRibbonClientConfig {
//
//    @Bean
//    public ServerListUpdater ribbonServerListUpdater() {
//        return new EurekaNotificationServerListUpdater();
//    }
//
//}
