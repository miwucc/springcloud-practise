package com.neo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

//@RestController 这个等于加了一个@ResponseBody会固定进行body序列化返回，无法进行redirect
@Controller
public class HelloController {

    @Autowired
    private Registration registration; // 服务注册

//    @Autowired
//    private DiscoveryClient client; // 服务发现客户端


    @GetMapping("/test-gzip")
    @ResponseBody
    public String testGizp(){

        String content = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        //拼接10次模拟返回大数据,不压缩大概在100K
        for(int i=0;i<10;i++){
            content = content + content;
        }

        return content;
    }


    //测试通过自己的FEGIN拦截器之后，GET请求可以传递POJO,如果fegin那边不用拦截器处理，这儿会User里面的属性值都是只能得到null
    @GetMapping("/add-user")
    @ResponseBody
    public String addUser(User user){
        return "add user success!" + user.getName();
    }

    //下面的请求默认是/hello?name=xx
    @RequestMapping("/hello")
    @ResponseBody
    public String index(@RequestParam String name)
    {
        System.out.println(11);
        return "hello "+name+"，this is first messge from server1";
    }

    //下面的请求默认是/hello?name=xx
    @RequestMapping("/throwExp")
    @ResponseBody
    public String throwExp(@RequestParam String name)
    {
        System.out.println(22);
        throw new RuntimeException("this is test RuntimeException");
    }


    @RequestMapping(value="/hello-hystrix-test-redirect", method=RequestMethod.GET)
    public String helloHystrixTestRedirect(@RequestParam String name) throws InterruptedException{
        return "redirect:/hello?name=这是测试重定向";
    }

    //下面的请求默认是/hello?name=xx
    @RequestMapping(value="/hello-hystrix-test", method=RequestMethod.GET)
    @ResponseBody
    public String helloHystrixTest(@RequestParam String name) throws InterruptedException {

        if(name.equals("yuanduanyichang")){
            throw new RuntimeException("我是远端抛出的异常");
        }

//        ServiceInstance instance = client.getLocalServiceInstance();

        int sleepMileSec = 10;
        if(name.equals("moniyanchi")){
            sleepMileSec = 500000;
        }
        System.out.println("start sleep "+sleepMileSec);

        Thread.sleep(sleepMileSec);

        System.out.println("/hello-hystrix-test success!af sleep milesec="+sleepMileSec);

//        System.out.println("/hello-hystrix-test host= "+instance.getHost()+",serverId="+instance.getServiceId());

        return "hello "+name+"，this is helloHystrixTest messge";

    }

    @RequestMapping("/noResponse")
    @ResponseBody
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