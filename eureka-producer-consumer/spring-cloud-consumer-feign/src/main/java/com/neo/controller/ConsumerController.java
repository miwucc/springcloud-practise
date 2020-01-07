package com.neo.controller;

import com.neo.remote.HelloRemote;
import org.springframework.beans.factory.annotation.Autowired;
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