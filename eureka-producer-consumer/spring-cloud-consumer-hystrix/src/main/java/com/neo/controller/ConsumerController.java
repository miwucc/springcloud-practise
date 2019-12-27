package com.neo.controller;

import com.neo.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Observable;
import rx.Observer;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
public class ConsumerController {

    @Autowired
    HelloService helloService;

    //测试同步调用返回，业务执行和会用tomcat线程池主线程
    @RequestMapping("/hello/test-hystrix-sync/{name}")
    public String testHystrixSync(@PathVariable("name") String name) {
        String a="";
        try{
            a = helloService.helloRemoteSerivceSync(name);
            System.out.println("controller完成任务,Thread="+Thread.currentThread().getName());
        }catch (Exception e){
            //如果没有fallback，但是远端又是直接抛出了异常，这里则会捕获httpFailException，内部不会包装
            throw e;
        }

        return a;
    }


    //测试异步调用，利用AsyncServlet特性，执行任务的时候采用spring单独的线程执行，response不关闭,执行完毕后再告知tomcat线程池线程进行response
    //返回是Callable(等同于Rannable，但是有返回结果)，DeferredResult(类似于callable，此类可以设置超时时长，等待结果后才返回单个结果)，ResponseBodyEmitter(可以返回多个结果)，SseEmitter，StreamingResponseBody的时候会采用异步servelt来搞，
    //异步servelt只是不会把tomcat线程池占住，但是浏览器的http连接还是会一直hold主等待结果返回
    //注意，异步调用因为切换了线程，会有threadlocal无法传递的问题

    @RequestMapping("/hello/test-hystrix-async-defer/{name}")
    public DeferredResult<String> testHystrixAsyncDefer(@PathVariable("name") String name){

        //此类天生多线程安全，专门用于异步返回结果的
        DeferredResult<String> deferredResult = new DeferredResult<String>(5000L);
        //设置超时后的处理
        deferredResult.onTimeout(new Runnable() {
            @Override
            public void run() {
                System.out.println("超时了！5S都没返回！");
                deferredResult.setResult("超时了！5S都没返回！");
            }
        });

        //这儿futrue会马上返回
        Future<String> asyncResult = helloService.helloRemoteSerivceAsync(name);

        //开一个线程来等待结果
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    //这儿只能获取到结果，如果内部执行异常了，则获取的是异常后rxjava把异常信息打印后传递出来的String

                    String result = asyncResult.get();
                    System.out.println("执行future结果，Thead="+Thread.currentThread().getName());
                    deferredResult.setResult(result.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("执行future结果异常，Thead="+Thread.currentThread().getName());
                    deferredResult.setResult("InterruptedException异常了！"+e.getMessage());
                //内部执行的异常，又没有fallback的会从这儿抛出来，一般会抛成java.util.concurrent.ExecutionException: Observable onError，futrue内部已经包装了，收不到原始的异常信息了
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    System.out.println("执行future结果异常，Thead="+Thread.currentThread().getName());
                    deferredResult.setResult("ExecutionException异常了！"+e.getMessage());
                }
            }
        },"等待结果线程").start();

        //马上返回结果,释放tomcat线程,但同时也会有一个taskExceutor线程来等待结果被设置
        return deferredResult;
    }

    //只用servelt层异步 tomcat线程 -> spring TaskExecutor线程
    @RequestMapping("/hello/test-hystrix-sync-callable/{name}")
    public Callable<String> testHystrixSyncCallable(@PathVariable("name") String name){
        Callable<String> callable = () -> {
            Thread.sleep(5000);
            System.out.println("实际工作开始执行！Thread="+Thread.currentThread().getName());
            String result="default";
            try{
                result = helloService.helloRemoteSerivceSync(name);
            }catch (Exception e){
                //这里如果是内部有执行异常，而fallback又没捕捉的则会在这里捕捉
                e.printStackTrace();
                System.out.println("捕捉了执行中的异常"+e.getMessage()+",Thread="+Thread.currentThread().getName());
                result = "捕捉了执行中的异常"+e.getMessage();
            }
            System.out.println("实际工作执行完成！");
            return result;
        };
        System.out.println("Controller执行结束,Thread="+Thread.currentThread().getName());
        //返回后，会创建一个spring TaskExecutor线程来执行callable的内容
        return callable;
    }


    //测试observe响应式方式，同样采用AsyncServelt特性执行，等待多个激发结果出来后再返回给前端连接
    //这儿只有两层线程 tomcat thread ->hystrix隔离线程
    @RequestMapping("/hello/test-hystrix-observable/{name}")
    public SseEmitter testHystrixAsyncObservable(@PathVariable("name") String name){

        //可以异步返回多个结果
        SseEmitter sseEmitter = new SseEmitter();

        //设置好订阅者后马上返回，tomcat主线程结束，等业务线程执行完毕后，再返回给前端结果
        Observable<String> observable2 = helloService.helloRemoteSerivceObserveAble(name);

        //需要先把sseEmitter返回的了，所以这里订阅结果放到一个单独的线程里面干
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                observable2.subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("订阅者-内部任务执行完成!ThreadName="+Thread.currentThread().getName());
                        sseEmitter.complete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        //因为采用了异步编程模型，内部没被fallback处理抛出的异常，这里会被包装成HystrixRuntimeException获取，原始异常信息会拿不到了
                        //HystrixRuntimeException 比如helloRemoteSerivceObserveAble timed-out and no fallback available.
                        try {
                            sseEmitter.send("订阅者-收到异常了！"+e.getMessage()+",ThreadName="+Thread.currentThread().getName());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }

                    @Override
                    public void onNext(String s) {
                        try {
                            System.out.println("订阅者-内部任务完成一次发射!ThreadName="+Thread.currentThread().getName());
                            sseEmitter.send(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        System.out.println("controller 结束!ThreadName="+Thread.currentThread().getName());

        //返回之后，tomcat线程池释放，同时response会等待被业务完成后线程进行调用
        return sseEmitter;


    }

}