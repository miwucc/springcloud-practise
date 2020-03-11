package com.neo.controller;

import com.neo.remote.HelloRemote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
public class ConsumerController {

    @Autowired
    HelloRemote helloRemote;

    @RequestMapping("/test-feign-gzip")
    public String testFeignGzip(HttpServletRequest request) {
        System.out.println(1111);
        String a= helloRemote.testGzip();
        return a;
    }

    @PostMapping("/test-feign-gzip-request")
    public String addUserGzip(User user){
        return helloRemote.addUser(user);
    }
	
    @RequestMapping("/hello/{name}")
    public String index(@PathVariable("name") String name) {
        return helloRemote.hello(name);
    }

    //这里springmvc支持再GET的时候把？后面的参数组合拼成一个POJO传参，但是这里往后调用fegin的时候，就不支持了，会报错，所以自定义了FeignRequestGetPOJOInterceptor来解决这个问题
    @GetMapping("/add-user")
    public String addUser(User user){
        return helloRemote.addUser(user);
    }


    @RequestMapping("/hello-hystrix/{name}")
    public String helloystrix(@PathVariable("name") String name) {
        String result="default";
        try{
            //开启了sentinel-feign之后，所有的feign自动生成entry资源，只需要在dashboard做流量配置就可以了
            result =  helloRemote.helloHystrixTest(name);
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    @RequestMapping("/hello-hystrix-redirect/{name}")
    public String helloystrixRedirect(@PathVariable("name") String name) {
        String result="default";
        try{
            result =  helloRemote.helloHystrixTestRedirect(name);
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }






}