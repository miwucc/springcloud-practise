package com.neo;

import com.google.common.cache.LoadingCache;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.lease.Lease;
import com.netflix.eureka.registry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cloud.netflix.eureka.server.EurekaServerConfigBean;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistry;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EurekaStateChangeListener implements ApplicationContextAware {

    private ApplicationContext ctxt;


    //获取服务注册列表根实际实现类对象
    @Autowired
    PeerAwareInstanceRegistry peerAwareInstanceRegistry;


    private static final Logger logger = LoggerFactory.getLogger(EurekaStateChangeListener.class);

    @PostConstruct
    public void init(){
        try {


            Field responseCacheFiled = AbstractInstanceRegistry.class.getDeclaredField("responseCache");
            responseCacheFiled.setAccessible(true);
            ResponseCache responseCacheImpl =  (ResponseCache)ReflectionUtils.getField(responseCacheFiled,peerAwareInstanceRegistry);

            //说明不是用responseCache缓存
            if(responseCacheImpl==null){

            //说明使用responseCache三级缓存
            }else{

            }



//            Field shouldUseReadOnlyResponseCacheFiled =  ResponseCacheImpl.class.getDeclaredField("shouldUseReadOnlyResponseCache");
//            InstanceRegistry instanceRegistry = ctxt.getBean(InstanceRegistry.class);

            System.out.println(responseCacheImpl);


//            .getBean(ResponseCacheImpl.class);

//             Field shouldUseReadOnlyResponseCacheFiled =  ResponseCacheImpl.class.getDeclaredField("shouldUseReadOnlyResponseCache");
//             shouldUseReadOnlyResponseCacheFiled.setAccessible(true);
//            shouldUseReadOnlyResponseCacheFiled.get()
//
//            Method method = ResponseCacheImpl.class.getDeclaredMethod("refreshRegistry");
//            method.setAccessible(true);
//            method.invoke(SpringUtil.getBean(DiscoveryClient.class));
        } catch (Exception e) {
            e.printStackTrace();
        }



    }



    /**
     * 服务下线事件
     * @param event
     */
    @EventListener
            (condition = "#event.replication==false")
    public void listenDown(EurekaInstanceCanceledEvent event){


        Field responseCacheFiled = null;
        try {
            responseCacheFiled = AbstractInstanceRegistry.class.getDeclaredField("responseCache");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        responseCacheFiled.setAccessible(true);
        ResponseCache responseCacheImpl =  (ResponseCache)ReflectionUtils.getField(responseCacheFiled,peerAwareInstanceRegistry);

        //说明不是用responseCache缓存
        if(responseCacheImpl==null){

            //说明使用responseCache三级缓存
        }else{

        }


        //只处理非复制事件，有多少个replicate这里就可能受到多少次
        if(!event.isReplication()){
            // 发送邮件
            logger.info(MarkerFactory.getMarker("DOWN"), "服务ID：" + event.getServerId() + "\t" +
                    "服务实例：" + event.getAppName() + "\t服务下线");
            logger.info(event.getServerId() + "\t" + event.getAppName() + "服务下线");
        }

    }

    /**
     * 服务注册事件
     * @param event
     */
    @EventListener
            (condition = "#event.replication==false")
    public void listenUp(EurekaInstanceRegisteredEvent event) {

        Field responseCacheFiled = null;
        try {
            responseCacheFiled = AbstractInstanceRegistry.class.getDeclaredField("responseCache");
            responseCacheFiled.setAccessible(true);
            ResponseCache responseCacheImpl =  (ResponseCache)ReflectionUtils.getField(responseCacheFiled,peerAwareInstanceRegistry);

            Method getCacheUpdateTaskMethod = ResponseCacheImpl.class.getDeclaredMethod("getCacheUpdateTask");
            getCacheUpdateTaskMethod.setAccessible(true);

            //responseCache缓存在第一次注册之后才会生成
            //TODO 需要先判断是否有打开readOnly缓存，如果有再调用刷新readOnly缓存的方法
            if(responseCacheImpl!=null) {

                //立即刷新缓存
                TimerTask task = (TimerTask) getCacheUpdateTaskMethod.invoke(responseCacheImpl);
                task.run();

                //同时通知各个其他注册者,有新注册上来了请刷新列表
            }

            //遍历当前实际注册服务，一个个通知当前服务发生改变
            Field  registryFiled =  AbstractInstanceRegistry.class.getDeclaredField("registry");
            registryFiled.setAccessible(true);
            ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry = (ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>>)registryFiled.get(peerAwareInstanceRegistry);
            for (Map<String, Lease<InstanceInfo>> leaseMap : registry.values()) {
                for (Map.Entry<String, Lease<InstanceInfo>> leaseEntry : leaseMap.entrySet()) {
                    Lease<InstanceInfo> lease = leaseEntry.getValue();
                    if(lease.getHolder()!= null){
                        //TODO需要过滤掉eurekapeer和注册上来的实例本身，通知其他所有注册方
                        InstanceInfo instanceInfo = (InstanceInfo)lease.getHolder();
                        System.out.println("当前注册实例："+instanceInfo.getAppName()+","+instanceInfo.getIPAddr()+":"+instanceInfo.getPort()+","+instanceInfo.getInstanceId());
                    }
                }
            }



        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }





        InstanceInfo instanceInfo = event.getInstanceInfo();

        if(!event.isReplication()){
            logger.info(MarkerFactory.getMarker("UP"), instanceInfo.getAppName() + "服务注册");
        }


    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctxt = applicationContext;
    }
}
