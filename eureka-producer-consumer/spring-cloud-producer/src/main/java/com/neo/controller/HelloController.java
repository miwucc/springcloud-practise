package com.neo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class HelloController {

    @Autowired
    private Registration registration; // 服务注册

//    @Autowired
//    private DiscoveryClient client; // 服务发现客户端

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

    //下面的请求默认是/hello?name=xx
    @RequestMapping(value="/hello-hystrix-test", method=RequestMethod.GET)
    public String helloHystrixTest(@RequestParam String name) throws InterruptedException {

//        ServiceInstance instance = client.getLocalServiceInstance();

        int sleepMileSec = new Random().nextInt(3000);
        System.out.println("start sleep "+sleepMileSec);

        Thread.sleep(sleepMileSec);

        System.out.println("/hello-hystrix-test success!af sleep milesec="+sleepMileSec);

//        System.out.println("/hello-hystrix-test host= "+instance.getHost()+",serverId="+instance.getServiceId());

        return "hello "+name+"，this is helloHystrixTest messge";

    }

    @RequestMapping("/noResponse")
    public void noResponse(@RequestParam String name)
    {
        System.out.println("noResponse!");
    }


//    public ServiceInstance serviceInstance() {
//
//        List<ServiceInstance> list = client.getInstances(registration.getServiceId());
//        if (list != null && list.size() > 0) {
//            for(ServiceInstance itm : list){
//                if(itm.getPort() == 2001)
//                    return itm;
//            }
//        }
//        return null;
//    }
}