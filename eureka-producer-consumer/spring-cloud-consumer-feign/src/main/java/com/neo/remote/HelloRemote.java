package com.neo.remote;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by summer on 2017/5/11.
 */
//通过FeignClient注解来来指定服务名字来绑定服务
@FeignClient(name= "spring-cloud-producer", fallback = HelloRemoteHystrix.class)
public interface HelloRemote {

    //使用springMVC注解来绑定该服务提供的REST接口
    @RequestMapping(value = "/hello")
    public String hello(@RequestParam(value = "name") String name);

}
