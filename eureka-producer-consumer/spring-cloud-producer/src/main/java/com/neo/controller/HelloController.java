package com.neo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    //下面的请求默认是/hello?name=xx
    @RequestMapping("/hello")
    public String index(@RequestParam String name)
    {
        System.out.println(11);
        return "hello "+name+"，this is first messge";
    }

    //下面的请求默认是/hello?name=xx
    @RequestMapping("/throwExp")
    public String throwExp(@RequestParam String name)
    {
        System.out.println(22);
        throw new RuntimeException("this is test RuntimeException");
    }
}