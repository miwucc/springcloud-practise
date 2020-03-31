package com.neo.service;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Description 代码继承接口的方式来实现断路器的调用的示例，如果有不清楚的可以看官方github示例
 * https://github.com/Netflix/Hystrix/tree/master/hystrix-examples/src/main/java/com/netflix/hystrix/examples/basic
 * @Author xl
 * @Date 2019/12/23
 */
//模拟hystrix注解的底层机制，实际是把调用请求封装为HystrixCommand 或 HystrixObservableCommand类实例，采用命令模式来进行请求
//HystrixCommand 用在依赖服务返回单个操作结果的时候
//HystrixObservableCommand 用在依赖服务返回多个(多次)操作结果的时候
//底层都是一来的rxjava响应式编程
//单个command对象里面有自己的统计器信号量或者线程池隔离引用
//下面是展示的用实现接口的来使用断路器，一般使用推荐注解方式，不和feign配合使用，这样也适用于用断路器封装三方外部api请求来达到断路效果
public class HelloCommandCodeService {

    /**实现单返回接口，实现一个hystrix命令类，必须要有run方法和构造函数，只会发射一次执行结果*/
   public static class  CommandHelloWorldExeOnce extends HystrixCommand<String> {

       private final String name;

       public CommandHelloWorldExeOnce(String name) {
           super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
            //父类构造函数中可以设置setter等等隔离级别策略等
//           super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
//                   // since we're doing work in the run() method that doesn't involve network traffic
//                   // and executes very fast with low risk we choose SEMAPHORE isolation
//                   .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
//                           .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)));

           this.name = name;
       }

       @Override
       protected String run() throws Exception {
           // a real example would do work like a network call here
           if(name.equals("Bob")||name.equals("World-Observe")){
               throw new RuntimeException("bob throw runtimeException");
           }
           return "Hello " + name + "!";
       }

       //如有必要，则复写降级方法
        @Override
        protected String getFallback() {
//            return super.getFallback();
            return "this is the fallback result!";
        }
    }

    /**实现多返回接口，必须实现自己的构造器和结接口的构造器，等同于run方法，里面可以用onNext发射多次执行结果*/
    public static class CommandHelloWorldExeMore extends HystrixObservableCommand<String> {

        private final String name;

        public CommandHelloWorldExeMore(String name) {
            super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
            this.name = name;
        }

        @Override
        protected Observable<String> construct() {
            return Observable.create(new Observable.OnSubscribe<String>() {
                @Override
                public void call(Subscriber<? super String> observer) {
                    try {
                        if (!observer.isUnsubscribed()) {
                            // a real example would do work like a network call here
                            //调用一次onNext发射一次通知
                            observer.onNext("Hello");
                            observer.onNext(name + "!");
                            observer.onCompleted();
                        }
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                }
            } ).subscribeOn(Schedulers.io());
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {


        //##############################执行一次CommandHelloWorldExeOnce测试

        //同步调用
        CommandHelloWorldExeOnce ch1 = new CommandHelloWorldExeOnce("Bob");
        System.out.println("result="+ch1.execute());//这里会打印fallback的返回值

        //异步调用
        Future<String> fr =  new CommandHelloWorldExeOnce("World").queue();
        System.out.println("result="+fr.get(1000,TimeUnit.SECONDS));

        //其实上面两个是一样的，就是看等待futrue的时间设置超时时间不，如果future不get那就纯异步了，因为主线程不会等待

//      //测试observable模式,还有个toObservable方法，区别就是observe不会等待订阅者订阅，直接就先执行命令，toObservable需要等待订阅者订阅后才会开始执行命令
        //其实oberve()里面还是调用的toObservale方法，先用一个可重放的订阅者(ReplaySubject)做buffer,先订阅接着，再给真正的订阅者
        //区别就是一个先执行run或者construct，然后执行订阅，把结果返给订阅者，一个先订阅，然后才执行run和construct
        Observable<String> fWorld = new CommandHelloWorldExeOnce("World-Observe").observe();
        Observable<String> fBob = new CommandHelloWorldExeOnce("Bob-Observe").observe();

        //blocking等待模式，single方法只能接收一次通知，如果要接收多次通知则会报错
        System.out.println(fWorld.toBlocking().single());
        System.out.println(fBob.toBlocking().single());

        //non-blocking模式，对上面的可观察者对象进行订阅，这样可以做到完全的异步通知得到结果
        fWorld.subscribe(new Observer<String>() {

            @Override
            public void onCompleted() {
                // nothing needed here
                System.out.println("fWorld complete non-blocking!");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("fWorld onError non-blocking!,"+e.getMessage());
                e.printStackTrace();
            }

            //这里会获取正常值或者fallback之后的值
            @Override
            public void onNext(String v) {
                System.out.println("fWorld onNext: " + v+" non-blocking");
            }

        });

        //ignore errors and onCompleted signal
        fBob.subscribe(new Action1<String>() {

            @Override
            public void call(String v) {
                System.out.println("fBob onNext: " + v+" non-blocking");
            }

        });


        System.out.println("分割线------------------------------------------------------");

        //##############################执行一次CommandHelloWorldExeMore测试

        //同步调用,一旦里面发射了多次，这种也要报错，这种只能适用于一次发射只有一次结果的情况
        CommandHelloWorldExeMore ch2 = new CommandHelloWorldExeMore("Bob Observe interface");
//        Future<String> future = ch2.observe().toBlocking().toFuture();
////        Future<String> future = ch2.toObservable().toBlocking().toFuture();
//        System.out.println("observable模式，同步等待结果=" + future.get());

        Observable<String> ch2Observe =  ch2.toObservable();
        ch2Observe.subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                System.out.println("observie more onNext: " + s);
            }
        });

    }

/***
 * execute()、queue()、observe()、toObservable()这4个方法用来触发执行run()/construct()，一个实例只能执行一次这4个方法，特别说明的是HystrixObservableCommand没有execute()和queue()。
 *
 * 4个方法的主要区别是：
 *
 * execute()：以同步堵塞方式执行run()。以demo为例，调用execute()后，hystrix先创建一个新线程运行run()，接着调用程序要在execute()调用处一直堵塞着，直到run()运行完成
 *
 * queue()：以异步非堵塞方式执行run()。以demo为例，一调用queue()就直接返回一个Future对象，同时hystrix创建一个新线程运行run()，调用程序通过Future.get()拿到run()的返回结果，而Future.get()是堵塞执行的
 *
 * observe()：事件注册前执行run()/construct()。以demo为例，第一步是事件注册前，先调用observe()自动触发执行run()/construct()（如果继承的是HystrixCommand，hystrix将创建新线程非堵塞执行run()；如果继承的是HystrixObservableCommand，将以调用程序线程堵塞执行construct()），第二步是从observe()返回后调用程序调用subscribe()完成事件注册，如果run()/construct()执行成功则触发onNext()和onCompleted()，如果执行异常则触发onError()
 *
 * toObservable()：事件注册后执行run()/construct()。以demo为例，第一步是事件注册前，一调用toObservable()就直接返回一个Observable<String>对象，第二步调用subscribe()完成事件注册后自动触发执行run()/construct()（如果继承的是HystrixCommand，hystrix将创建新线程非堵塞执行run()，调用程序不必等待run()；如果继承的是HystrixObservableCommand，将以调用程序线程堵塞执行construct()，调用程序等待construct()执行完才能继续往下走），如果run()/construct()执行成功则触发onNext()和onCompleted()，如果执行异常则触发onError()
 *
 * 3、fallback（降级）
 * 使用fallback机制很简单，继承HystrixCommand只需重写getFallback()，继承HystrixObservableCommand只需重写resumeWithFallback()，比如HelloWorldHystrixCommand加上下面代码片段：
 *
 * @Override
 * protected String getFallback() {
 *     return "fallback: " + name;
 * }
 * fallback实际流程是当run()/construct()被触发执行时或执行中发生错误时，将转向执行getFallback()/resumeWithFallback()。结合下图，4种错误情况将触发fallback：
 *
 * 非HystrixBadRequestException异常：以demo为例，当抛出HystrixBadRequestException时，调用程序可以捕获异常，没有触发getFallback()，而其他异常则会触发getFallback()，调用程序将获得getFallback()的返回
 *
 * run()/construct()运行超时：以demo为例，使用无限while循环或sleep模拟超时，触发了getFallback()
 *
 * 熔断器启动：以demo为例，我们配置10s内请求数大于3个时就启动熔断器，请求错误率大于80%时就熔断，然后for循环发起请求，当请求符合熔断条件时将触发getFallback()。更多熔断策略见下文
 *
 * 线程池/信号量已满：以demo为例，我们配置线程池数目为3，然后先用一个for循环执行queue()，触发的run()sleep 2s，然后再用第2个for循环执行execute()，发现所有execute()都触发了fallback，这是因为第1个for的线程还在sleep，占用着线程池所有线程，导致第2个for的所有命令都无法获取到线程
 *
 * 链接：https://www.jianshu.com/p/b9af028efebb
 */

}
