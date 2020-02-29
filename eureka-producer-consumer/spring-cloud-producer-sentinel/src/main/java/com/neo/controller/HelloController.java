package com.neo.controller;

import com.neo.service.SentinelAnoService;
import com.neo.service.SentinelCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

//@RestController 这个等于加了一个@ResponseBody会固定进行body序列化返回，无法进行redirect
@Controller
public class HelloController {

    @Autowired
    private Registration registration; // 服务注册

    @Autowired
    SentinelAnoService sentinelAnoService;
    @Autowired
    SentinelCodeService sentinelCodeService;

//    @Autowired
//    SentinelCodeService sentinelCodeService;

    @RequestMapping(value="/test-sentinel", method=RequestMethod.GET)
    @ResponseBody
    public String testSentinel()
    {
        String result ="success";
        result = sentinelAnoService.hello(System.currentTimeMillis());
        return result;
    }


    @RequestMapping(value="/test-sentinel-code", method=RequestMethod.GET)
    @ResponseBody
    public String testSentinelCode()
    {
        String result ="success";
        result = sentinelCodeService.helloCodeSource();
        return result;
    }
}