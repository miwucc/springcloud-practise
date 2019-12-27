package com.neo.remote;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by summer on 2017/5/11.
 */
//通过FeignClient注解来来指定服务名字来绑定服务
@FeignClient(name= "spring-cloud-producer", fallback = HelloRemoteFallback.class)
public interface HelloRemote {

    //使用springMVC注解来绑定该服务提供的REST接口
    @RequestMapping(value = "/hello")
    public String hello(@RequestParam(value = "name") String name);


    //测试在feigin包装的情况下，hystrix熔断接口
    //默认情况下
    // commandGroupKey=spring-cloud-producer,
    // commandKey=HelloRemote#helloHystrixTest(String),
    // threadPoolKey=null
    //commandProperties，ThreadPoolProperties 均为null
    //默认采用的SetterFactory.Default这个内部类来做的commond setter，只会设置groupkey和comandKey，其它属性不会设置
    @RequestMapping(value="/hello-hystrix-test", method=RequestMethod.GET)
    public String helloHystrixTest(@RequestParam(value = "name") String name);


}
