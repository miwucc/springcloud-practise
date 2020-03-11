package com.neo.remote;

import com.neo.controller.User;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by summer on 2017/5/11.
 */
//通过FeignClient注解来来指定服务名字来绑定服务
//需要开启404解码器，以免4040的时候会记录熔断统计，几个404就进熔断了,源码在SynchronousMethodHandler#invoke #executeAndDecode
//@FeignClient(name= "spring-cloud-producer", fallback = HelloRemoteFallback.class, decode404=true)
//要获取抛出的异常信息，也就是引起fallback的原因,就需要使用fallbackFactory而不是fallback

/***
 * hystrix的异常处理，只要不是HystrixBadRequestException，比如本地run方法中抛出的本地业务异常，断路器的并发控制异常，超时异常等的都会跑到fallback去执行，
 * 如果没有fallback，则会抛出给调用方函数来自行捕捉异常
 *
 * 注意超时异常假如在正常返回之前触发，则会优先通过Hystrix-timer线程会优先触发超时异常，进入超时异常的fallback进行执行，返回fallback的信息，
 * 再等到hystrix的隔离业务线程组真正返回的http结果，比如500的结果，则不会被返回，也不会被打印，会被吃掉。你就看不到本次远程调用的结果。
 *
 * 如果不做fallback，则会直接抛出，需要在业务调用层做catch控制
 *
 * 目前看来比较好的做法是，在feign中使用HelloRemoteFallbackFactory，对抛出的异常进行log打印，这样方便出现问题后在日志层进行查看。
 * 有需要业务处理的fallback则进行处理，没有的则抛出自己的转换后的业务异常。
 *
 * 同时实现自己的ErrorDecoder，对于非200状态中的4xx状态包装为HystrixBadRequestException抛出，以免进入断路器计算。
 * 对于http返回的其它状态，只要超时时间设得够长，暂时不进行打印，
 *
 */
@FeignClient(name= "spring-cloud-producer"
, fallbackFactory = HelloRemoteFallbackFactory.class
, decode404=true
//,configuration = CustomerFeignConfiguration.class //如果不用默认或全局配置，则需要在这里进行指定
)
public interface HelloRemote {

    @RequestMapping(value = "/test-gzip")
    public String testGzip();

    //使用springMVC注解来绑定该服务提供的REST接口
    @RequestMapping(value = "/hello")
    public String hello(@RequestParam(value = "name") String name);

    //测试在feigin包装的情况下，hystrix熔断接口
    //默认情况下
    // commandGroupKey=spring-cloud-producer,
    // commandKey=HelloRemote#helloHystrixTest(String),
    // ThreadPoolKey=commandGroupKey
    @RequestMapping(value="/hello-hystrix-test", method=RequestMethod.GET)
    public String helloHystrixTest(@RequestParam(value = "name") String name);

    @RequestMapping(value="/hello-hystrix-test-redirect", method=RequestMethod.GET)
    public String helloHystrixTestRedirect(@RequestParam(value = "name") String name);

    /**
     * GET 方式，默认情况是不允许用GET方式传递POJO的，底层fegin会默认切换成POST，不过这里用了自己的拦截FeignRequestGetPOJOInterceptor来进行GET传递POJO
     * @param user user
     * @return 添加结果
     */
    @RequestMapping(value = "/add-user", method = RequestMethod.GET)
    String addUser(User user);

}
