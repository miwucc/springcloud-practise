package com.neo.service;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.annotation.ObservableExecutionMode;
import com.netflix.hystrix.contrib.javanica.command.AsyncResult;
import org.omg.SendingContext.RunTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Future;

@Service
public class HelloService {

    @Autowired
    RestTemplate restTemplate;

    /**
     * 给可能失败的方法加上@HystrixCommand命令从而可以被断路器接管
     * */
    //请求命令同步执行
    @HystrixCommand(fallbackMethod = "helloRemoteSerivceFallback"
            //定义名字组和名字名字，默认采用方法名，hystrix会根据组来统计仪表盘数据，而当threadPoolKey不单独定义的时候，命令线程组是直接按groupKey来划分的，如果定义了threadPoolKey则按自定义的来进行划分隔离
//            ,groupKey = "helloRemoteSerivceGroup"
//            ,commandKey="helloRemoteSerivce"
//            ,threadPoolKey = "helloRemoteSerivceThread"
//    ,ignoreExceptions = XXXException.class 当这个异常抛出的时候不促发降级方法
            //配置各种属性
//            ,commandProperties = {@HystrixProperty(name="execution.isolation.thread.timeoutInMillseconds",value="5000") }
            //hystrix的配置范围注意有以下4种： 1.全局默认配值(代码默认值) 2.全局配置属性(config中心中配置的，可以下发刷新) 3.实例默认值(代码值) 4.实例配置属性(config中心中配置的，可以下发刷新)
//            Commond属性：
//                execution 隔离策略相关，默认线程池，可选信号量。默认线程池，开启命令执行超时时间1000毫秒，同时最大并发请求10
//                fallback 降级方法相关，降级方法默认启用，最大并发调用数=10
//                circuitBreaker 短路器设置，默认使用断路器，5s内至少20个请求，失败50%才打开断路器,一个时间窗口后发现激活断路器后则进行溶断打开断路，直到下一个时间窗口统计发现ok再次尝试，则关闭断路器
//                metrics 性能统计相关，统计滑动窗口时间段，分为多少个桶做统计等
//                requestContext 是否开启缓存，是否打印requestlog
//            collapser属性（对应@HystrixCollapser注解中的，collapserProperties,注意是请求commond合并相关设置）：
//            threadPool属性(对应threadPoolProperties)，默认10个线程，不开启线程池队列
//更多阅读:https://www.jianshu.com/p/b6e9706f382c
//https://www.jianshu.com/p/6c574abe50c1
    )
    public String helloRemoteSerivce(String name){
        long start = System.currentTimeMillis();
        String resultStr = restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
        long end = System.currentTimeMillis();
        //hystrix默认超时时间是1s，可以看到spend time>1s的都会触发服务降级
        System.out.println("spend time="+(end-start));

        return resultStr;
    }

    /**降级方法，fallback方法必须和元方法在定义在同一个类中，修饰符不做限制 方法在正常方法执行中出现错误，超时，线程池拒绝，断路器熔断等请看下促发执行*/
    /**fallback方法的参数必须要和被fallback的方法保持一致*/
    /**有没有降级方法都不影响在方法执行失败后，断路器状态变更,所以，一般对于写方法，或者执行批处理或离线计算的方法，能返回调用者知道失败就行
     * 用不着必须要降级方法*/
    public String helloRemoteSerivceFallback(String name,Throwable ex){

        if(ex!=null && ex instanceof RuntimeException){
            return "catch exception and make the fallback!";
        }
        return "error and fire fallback!name="+name;

    }

    //请求命令异步执行
    @HystrixCommand
    public Future<String> helloRemoteSerivceAsync(String name){
        return new AsyncResult<String>() {
            @Override
            public String invoke() {
                return restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
            }
        };
    }

    //响应式命令执行 toObserve模式
    @HystrixCommand(observableExecutionMode=ObservableExecutionMode.EAGER)
    public String helloRemoteSerivceObserve(String name){
        return restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
    }

    //响应式命令执行 toObserveable模式
    @HystrixCommand(observableExecutionMode=ObservableExecutionMode.LAZY)
    public String helloRemoteSerivceObserveable(String name){
        return restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
    }


}
