package com.neo.controller;

import com.neo.controller.ribbonrule.RibbonUserIdHolder;
import com.neo.remote.HelloRemote;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@RestController
public class ConsumerController {

    @Autowired
    HelloRemote helloRemote;

    @Autowired
    RestTemplate restTemplate;

    @RequestMapping("/test-feign-gzip")
    public String testFeignGzip(HttpServletRequest request) {
        System.out.println(1111);
        String a= helloRemote.testGzip();
        return a;
    }

    public static int betweenInt(int min, int max, boolean include) {
        // 参数检查
        if (min > max) {
            throw new IllegalArgumentException("最小值[" + min + "]不能大于最大值[" + max + "]");
        } else if (!include && min == max) {
            throw new IllegalArgumentException("不包括边界值时最小值[" + min + "]不能等于最大值[" + max + "]");
        }
        // 修正边界值
        if (include) {
            max++;
        } else {
            min++;
        }
        return (int) (min + Math.random() * (max - min));
    }


    /**模拟300-500ms的IO阻塞(不消耗CPU),有线程上下文切换，的调用*/
    @RequestMapping("/test-thread")
    public String testThread(HttpServletRequest request) {

        String a = request.getRequestURI();
        if(a.endsWith("/test-thread")){
            System.out.println(111);
        }

        String b = request.getServletPath();
        if(b.endsWith("/test-thread")){
            System.out.println(111);
        }

        long sleepMillis = betweenInt(200,500,true);

        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return "success ! sleep="+sleepMillis;
    }

    public static void main(String[] args) {
        BigDecimal a = new BigDecimal(0);
        System.out.println(a.compareTo(BigDecimal.ZERO));
    }

    /**直接返回调用*/
    @RequestMapping("/hello/local/{name}")
    public String indexLocal(@PathVariable("name") String name, HttpServletRequest request) {

        String sessionId = request.getSession().getId();
        System.out.println("sessionId="+sessionId);

        return "success ! "+name;
    }

    /**直接返回抛出异常*/
    @RequestMapping("/hello/localExp/{name}")
    public String indexLocalExp(@PathVariable("name") String name) {
        throw new RuntimeException("this is test Exp");
    }

    /**测试FeignClient的方式远程调用*/
    @RequestMapping("/hello/{name}")
    public String index(@PathVariable("name") String name, HttpServletRequest request) {

        String sessionId = request.getSession().getId();
        System.out.println("sessionId="+sessionId);
        //故意设置一个用户ID，用ThreadLocal传到ribbon里面进行分片负债均衡
        int userID = 1002;
        RibbonUserIdHolder.put(userID);

        return helloRemote.hello(name);
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

    /**异常的处理方式有两种：
     * 一种是自己实现一个FeignException的继承异常类，用自己的ErrorDecoder来解析
     * 异常并返回，在调用端捕捉后做相应处理。比如下面的代码。
     * 一种是在被调用端统一处理调用函数，对调用函数抛出的异常都做捕捉然后封装成一个对象，比如XXResponse，
     * 然后调用端假如catch到了其它Exception，则就说明不是业务异常，是网络一类的，业务是否异常全部在XXResponse里面根据返回的data和code来组判断
     *
     *
     * */
    @RequestMapping("/testThrowExpByFeign/{name}")
    public String index4(@PathVariable("name") String name) {
        String result="";
        try{
            //服务名访问地址+路径
            result =  helloRemote.throwExp(name);
        }catch (HttpServerErrorException ex){
            result = ex.getStatusCode()+" "+ex.getResponseBodyAsString();
        //如果是用feign调用，被调用方抛出了异常(实际是http500)，则feigin底层会包装成FeignException
        }catch(FeignException feignException){
            System.out.println("status:"+feignException.status()+",message:"+feignException.getMessage());
            if(feignException instanceof  MyFeignException){
                MyFeignException ex = (MyFeignException)feignException;
                result="status:"+ex.status()+",reason:"+ex.getExceptionContent().getMessage();
                System.out.println(result);
            }
        }catch (Exception e){
            result = "服务器异常";
        }
        return result;
    }

    @RequestMapping("/testNoResponseByFeign/{name}")
    public String index5(@PathVariable("name") String name) {
        String result=null;
        try{
            //服务名访问地址+路径
            helloRemote.noResponse(name);
        }catch (HttpServerErrorException ex){
            result = ex.getStatusCode()+" "+ex.getResponseBodyAsString();
        }catch(FeignException feignException){
            System.out.println("status:"+feignException.status()+",message:"+feignException.getMessage());
            if(feignException instanceof  MyFeignException){
                MyFeignException ex = (MyFeignException)feignException;
                result="status:"+ex.status()+",reason:"+ex.getExceptionContent().getMessage();
                System.out.println(result);
            }
        }catch (Exception e){
            result = "服务器异常";
        }
        return result;
    }

}