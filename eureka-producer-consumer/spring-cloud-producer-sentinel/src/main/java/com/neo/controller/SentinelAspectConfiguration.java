//package com.neo.controller;
//
//import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**当前配置下必须启用Sentinel的注解AOP配置，不然不会生效，官方示例里面也没写。。。
// * 如果你用starer的话有自动配置了也可以不自己写这个类
// *
// * */
//@Configuration
//public class SentinelAspectConfiguration {
//    @Bean
//    public SentinelResourceAspect sentinelResourceAspect() {
//        return new SentinelResourceAspect();
//    }
//
//}
