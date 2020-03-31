package com.neo.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.annotation.ObservableExecutionMode;
import com.netflix.hystrix.contrib.javanica.command.AsyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rx.Observable;
import rx.Subscriber;

import java.util.concurrent.Future;

/***
 * 用注解的方式来进行一个断路器命令包装执行
 * 注意，如果用线程隔离模式，因为内部执行用了线程池，threadlocal是无法传递的
 *
 */
@Service
public class HelloService {

    @Autowired
    RestTemplate restTemplate;

    /**
     * 下面包装x方法为hystrixcommand来执行，实现断路器功能
     * 给可能失败的方法加上@HystrixCommand命令从而可以被断路器接管
     * properties中针对commandKey的配置>注解命令中的代码配置>properties中的default全局配置>默认全局配置,properties中的配置都是可以用配置中心下发刷新的
     * */
    //请求命令同步执行
    @HystrixCommand(
//            groupKey = "helloRemoteSerivceGroup",//默认类名简写，这里就是"HelloService"
//            commandKey="helloRemoteSerivce",//默认方法名字简写，这里就是"helloRemoteSerivceSync"
//            threadPoolKey="helloRemoteSerivceThread"//默认null，但是最终设值的时候如果是null会用group名替代
            fallbackMethod = "helloRemoteSerivceFallback",//是否实现降级方法，默认不实现,run方法执行中，
            // 除了BadRequestException以外，默认都会用走fallback方法，
//            当异常抛出，如果你是同步调用，找不到fallback方法则会继续朝上抛,而如果是异步调用,会报observale.onerror异常打印到rx事件流中，所以，尽量所有都实现fallback方法再在里面根据异常进行处理返回
//    ,ignoreExceptions = XXXException.class 当这个异常抛出的时候不促发降级方法
            commandProperties = {
                //主要分为execution，fallback，circuitBreaker，metrics，requestContext,collapser几种设置
                @HystrixProperty(name="execution.isolation.strategy",value="THREAD"),//隔离级别策略，采用线程
                @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="4000"),//hystrix commond run超时时间默认1000ms
                @HystrixProperty(name="circuitBreaker.requestVolumeThreshold", value = "1"),//开启断路器条件之-时间窗口内请求数至少到达,默认20
                @HystrixProperty(name="circuitBreaker.errorThresholdPercentage", value="10"),//开启断路器条件之-时间窗口内失败百分比至少到达,默认50，需要和上面同时满足
                @HystrixProperty(name="circuitBreaker.sleepWindowInMilliseconds", value="10000"),//熔断持续多长时间才进行半开状态，默认5S
                @HystrixProperty(name="metrics.rollingStats.timeInMilliseconds",value="10000"),//失败统计时间窗口，默认10S
                @HystrixProperty(name="metrics.rollingStats.numBuckets",value="10")}//统计滑动窗口桶划分，必须能被上面这个整除,默认10
            //线程隔离级别的时候线程池相关配置
            ,threadPoolProperties={
                @HystrixProperty(name = "coreSize",value="10"),//默认10
//                @HystrixProperty(name="maximumSize", value="10"),//默认10，这个值目前不让设，估计是不会生效吧所以干脆就不让设了...
                 @HystrixProperty(name="maxQueueSize", value="-1")//默认-1，这个不要设置，推荐上面最小最大设置一样，目前有bug如果设置了队列数，队列满了也不会增加线程到最大值，所有队列不要设置>0m，同时core和max要设置为一样
            }
    )
//    @HystrixCollapser() 用于把多个command请求执行合并为一个，结果再拆分返回，需要自己实现相关逻辑，比如把几个按id查询的合成一个按idlist查询，一般不用，这里忽略
//    @CacheResult() 标识需要把方法返回结果存缓存，这里忽略
//    @CacheRemove() 标识缓存不启用失效，这里忽略
//    @CacheKey() 缓存key设置，这里忽略
    //更多阅读:https://www.jianshu.com/p/b6e9706f382c
//https://www.jianshu.com/p/6c574abe50c1

    /**最普通的同步调用方式 HystrixCommand execute执行 同步阻塞直至从依赖服务返回结果或抛出异常*/
    public String helloRemoteSerivceSync(String name){
        //这个方法执行如果设置的是线程池隔离模式，这儿是用的hystrix自己的壁仓线程池在执行
        long start = System.currentTimeMillis();
        if(name.endsWith("miwucc")){
            throw new RuntimeException("测试还未到远端就抛出的异常");
        }
        String resultStr = restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
        long end = System.currentTimeMillis();
        //hystrix默认超时时间是1s，可以看到spend time>1s的都会触发服务降级
        System.out.println("内部方法,spend time="+(end-start)+",Thread="+Thread.currentThread().getName());

        return resultStr;
    }

    /**只要不是HystrixBadRequestException,其它run时候产生的异常都会触发进入fallback方法，ex可能是被hystrix包装后的异常类型，比如远端异常，http状态是5xx，一般会被封装为HttpServerErrorException到这里
     * 根据ex来处理相关事务返回降级事务或者原地抛出*/
    public String helloRemoteSerivceFallback(String name,Throwable ex){

        //降级方法的结果会返回给调用线程，如果在这里直接抛出异常，也会直接抛给调用线程


        //如果是远端抛异常，这这里接到的是HttpServerErrorException
        //非远端抛出的异常，会原封不动的抛出来
        if(ex!=null && ex instanceof RuntimeException){
            System.out.println("catch exception and make the fallback!"+",ex="+ex.toString());
            return "catch exception and make the fallback!"+",ex="+ex.toString();
        }else{
            System.out.println("error and fire fallback!name="+name+",ex="+ex.toString());
        }
        return "error and fire fallback!name="+name+",ex="+ex.toString();

    }

    /**
     * 注解的入口类HystrixCircuitBreakerConfiguration，里面定义了了切面类HystrixCommandAspect，对有@HystrixCommand注解的方法进行扫描AOP处理
     * 看里面的methodsAnnotatedWithHystrixCommand方法，实际是吧aop切到的方法封装为MetaHolder，然后包装为HystrixInvokable来执行的，执行的时候需要执行类型ExecutionType
     * 执行类型分为ASYNCHRONOUS（asynchronous），SYNCHRONOUS(synchronous)，OBSERVABLE(asynchronous callback)三种，主要是根据切到的方法return类型来的
     * 看ExecutionType.getExecutionType方法，如果方法返回类型是java.util.concurrent.Future的子类，就用asynchronous方式。返回的是rx.Observable类型，就用OBSERVABLE方式，否则用同步方式
     * 上面三种方式最终都是调用的CommandExecutor.execute(invokable, executionType, metaHolder);
     * 同步方式最终就是调用的execute()=queue().get()
     * 异步方式就是调用的queue()
     * OBSERVABLE方式就是根据注解设定的用egear模式还是LAZY模式来决定先执行再订阅，还是先订阅再执行，对应 observable.observe() 还是 observable.toObservable()
     *
     * 下面三个方法分别就是演示了异步执行，obervable方看ExecutionType式的两种执行
     * */
    //异步执行
    @HystrixCommand
    public Future<String> helloRemoteSerivceAsync(String name){
        //这个方法执行如果设置的是线程池隔离模式，这儿是用的hystrix自己的壁仓线程池在执行
        System.out.println("helloRemoteSerivceAsync，Thead="+Thread.currentThread().getName());
        //返回的futrue自己外面再看用什么方式来异步获取结果
        Future future =  new AsyncResult<String>() {
            //这个方法执行如果设置的是线程池隔离模式，这儿是用的hystrix自己的壁仓线程池在执行
            @Override
            public String invoke() {
                if(name.equals("miwucc")){
                    throw new RuntimeException("future 形式调用下抛出异常！");
                }

                System.out.println("helloRemoteSerivceAsync AsyncResult内部,Thead="+Thread.currentThread().getName());

                return restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
            }
        };

        return future;
    }

    /**发布-订阅响应式调用,先执行，后订阅
    *HystrixObservableCommand observe执行，可以支持多个事件结果触发
    *响应式命令执行 toObserve模式
    */
    @HystrixCommand(
            observableExecutionMode=ObservableExecutionMode.EAGER)
    public Observable<String> helloRemoteSerivceObserve(String name){
        System.out.println("执行被Hystrix包裹的主方法方法,Thread="+Thread.currentThread().getName());

       return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {

                if(name.equals("miwucc")){
                    throw new RuntimeException("observe 形式调用下抛出异常！");
                }

                try{
                    //订阅者已经订阅后才执行，如果是egaer模式，其实是抢先会订阅一个重放订阅者
                    if(!observer.isUnsubscribed()){
                        String result1 = restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
                        observer.onNext(result1+"1");//发射第1次结果
                        String result2 = restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
                        observer.onNext(result2+"2");//发射第2次结果
                        observer.onCompleted();//完毕
                    }
                }catch (Exception e){
                    observer.onError(e);
                }
            }
        });


    }

    /**发布-订阅响应式调用
    *HystrixObservableCommand toObservable执行
    *响应式命令执行 toObserveable模式，先执行后订阅，可以支持多个事件结果触发
     *
     * Observale模式默认的隔离模式是SEMAPHORE信号量模式:
     * 采用EAGER模式时,线用主线程执行run，再用订阅者的线程执行订阅事务.
     * 采用LAZY模式时，订阅事务和run方法都采用订阅者的线程来执行
     *
     * 同理，调整为Thread隔离级别了之后，
     * 不管是lazy和爱是EAGER模式，则都会采用hystrix线程来执行run和订阅事务。
     * */
    @HystrixCommand(observableExecutionMode=ObservableExecutionMode.LAZY
            ,commandProperties = {
            @HystrixProperty(name = "execution.isolation.strategy", value = "THREAD")}
    )
    public Observable<String> helloRemoteSerivceObserveAble(String name){

        System.out.println("执行被Hystrix包裹的主方法方法,Thread="+Thread.currentThread().getName());

        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                try{
                    //
                    if(name.equals("miwucc")){
                      throw new RuntimeException("observe 形式调用下抛出异常！");
                    }

                    //订阅者已经订阅后才执行，如果是egaer模式，其实是抢先会订阅一个重放订阅者
                    if(!observer.isUnsubscribed()){
                        String result1 = restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
                        observer.onNext(result1+"1");//发射第1次结果
                        System.out.println("CALL内部执行一次发射，ThreadName="+Thread.currentThread().getName());
                        String result2 = restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
                        observer.onNext(result2+"2");//发射第2次结果
                        System.out.println("CALL内部执行一次发射，ThreadName="+Thread.currentThread().getName());
                        String result3 = restTemplate.getForEntity("http://spring-cloud-producer/hello-hystrix-test?name={1}",String.class,name).getBody();
                        observer.onNext(result3+"3");//发射第3次结果
                        System.out.println("CALL内部执行一次发射，ThreadName="+Thread.currentThread().getName());
                        observer.onCompleted();//完毕
                    }
                }catch (Exception e){
                    observer.onError(e);
                }
            }
        });
    }


}
