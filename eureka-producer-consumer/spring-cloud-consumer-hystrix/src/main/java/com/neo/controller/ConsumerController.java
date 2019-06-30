package com.neo.controller;

import com.neo.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerController {

    @Autowired
    HelloService helloService;

    @RequestMapping("/hello/testHystrix/{name}")
    public String testHystrix(@PathVariable("name") String name) {
        return helloService.helloRemoteSerivce(name);
    }

}