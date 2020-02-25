package com.neo.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaAutoServiceRegistration;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**尝试做一个关闭前第一个执行的钩子，代替htt盘点用pause endpoint功能，这样在kill的时候就先自动parse等x秒微服务下线后在继续后续清理工作
 * 不过这个建议放在k8s preStop钩子里直接调用endpoint端点比较好，因为这个类不一定所有工程都升级引入了，万一没引入就不支持。但是springboot和cloud引入了endpoint是支持的，并且可以在线热更新配置
 *  */
//定义为一个bean定义
@Component
public class EurekaClosePrePauseWaiter implements SmartLifecycle,Ordered {

    @Autowired
    EurekaAutoServiceRegistration registration;

    //默认false
    AtomicBoolean running = new AtomicBoolean();


    //不自动启动。只在关闭的时候做调用，所以关闭自动启动选项
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        //以免调用两次
        if(running.get()){
            System.out.println("我是第一个执行pause！start");
            registration.stop();
            running.set(false);
            System.out.println("我是第一个执行pause！end");
            System.out.println("我是第一个执行pause  sleep！start");
            //睡10s，等待其他服务感应微服务下线
            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("我是第一个执行pause  sleep！end");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    //放到第一阶段执行
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    //如果是调用shutdown endpoint方式关闭，在执行smartlife前会有ContextClosedEvent抛出，这个执行在smartlife之前，所以必须实现这个，让这个先执行
    @EventListener({ContextClosedEvent.class})
    public void onApplicationEvent(ContextClosedEvent event) {
        this.stop();
    }

    //控制上面ContextClosedEvent事件优先于eureka本身的shutdown endpoint触发的监听先处理
    @Override
    public int getOrder() {
        return -1;
    }
}
