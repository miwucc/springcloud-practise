package com.neo.controller;

import com.neo.remote.HelloRemote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ConsumerController {

    @Autowired
    HelloRemote HelloRemote;

    @Autowired
    RestTemplate restTemplate;
	
    @RequestMapping("/hello/{name}")
    public String index(@PathVariable("name") String name) {
        return HelloRemote.hello(name);
    }


    @RequestMapping("/hello2/{name}")
    public String index2(@PathVariable("name") String name) {
            //服务名访问地址+路径
        return restTemplate.getForEntity("http://spring-cloud-producer/hello?name={1}",String.class,name).getBody();
    }

}