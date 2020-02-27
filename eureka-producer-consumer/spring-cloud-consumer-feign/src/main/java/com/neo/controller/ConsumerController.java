package com.neo.controller;

import com.neo.remote.HelloRemote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerController {

    @Autowired
    HelloRemote helloRemote;
	
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