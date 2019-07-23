package com.neo.controller;

import com.neo.remote.HelloRemote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
public class ConsumerController {

    @Autowired
    HelloRemote HelloRemote;

    @Autowired
    RestTemplate restTemplate;

    /**直接返回调用*/
    @RequestMapping("/hello/local/{name}")
    public String indexLocal(@PathVariable("name") String name) {
        return "success ! "+name;
    }

    /**直接返回抛出异常*/
    @RequestMapping("/hello/localExp/{name}")
    public String indexLocalExp(@PathVariable("name") String name) {
        throw new RuntimeException("this is test Exp");
    }

    /**测试FeignClient的方式远程调用*/
    @RequestMapping("/hello/{name}")
    public String index(@PathVariable("name") String name) {
        return HelloRemote.hello(name);
    }

    /**测试采用restTemplate的方式远程调用*/
    @RequestMapping("/hello2/{name}")
    public String index2(@PathVariable("name") String name) {
            //服务名访问地址+路径
        return restTemplate.getForEntity("http://spring-cloud-producer/hello?name={1}",String.class,name).getBody();
    }

    /**测试远程调用抛出异常*/
    /**
     * 这个时候会报异常，需要catch相关异常然后根据返回处理，请参看DefaultResponseErrorHandler
     * org.springframework.web.client.HttpServerErrorException: 500 null
     * 	at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:66) ~[spring-web-4.3.8.RELEASE.jar:4.3.8.RELEASE]
     *
     *
     * */
    @RequestMapping("/testThrowExp/{name}")
    public String index3(@PathVariable("name") String name) {
        String result;
        try{
            //服务名访问地址+路径
           result =  restTemplate.getForEntity("http://spring-cloud-producer/throwExp?name={1}",String.class,name).getBody();
        }catch (HttpServerErrorException ex){
            result = ex.getStatusCode()+" "+ex.getResponseBodyAsString();
        }catch (Exception e){
            result = "服务器异常";
        }
        return result;
    }

}