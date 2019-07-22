package com.neo.insaware;

import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @Description eureka客户端用的服务上下线实时感知处理器 ,
 * 在收到eurekaServer发来的其它服务上下线通知后，及时更新自身的注册服务serverList列表
 * @Author xl
 * @Date 2019/7/22
 */

@RestController
public class EurekaInsAwareClientListener {
    private static final Logger log = LoggerFactory.getLogger(EurekaInsAwareClientListener.class);


    @Autowired
    @Qualifier("eurekaClient")
    private EurekaClient eurekaClientProxy;

    //收到注册中心发来的服务上线通知
    @RequestMapping("/notifyInsAware/up")
    @ResponseBody
    public String notifyUp(@RequestParam String appName,@RequestParam String instanceId){
        log.info("recived up notify,appName="+appName+",instanceId="+instanceId);
        //及时刷新注册信息列表，重新找eurekaServer拉一次
        refreshRegistry();
        return "success";
    }

    //收到注册中心发来的服务下线通知
    @RequestMapping("/notifyInsAware/cancel")
    @ResponseBody
    public String notifyDown(@RequestParam String appName,@RequestParam String instanceId) {
        log.info("recived cancel notify,appName="+appName+",instanceId="+instanceId);
        //及时刷新注册信息列表，重新找eurekaServer拉一次
        refreshRegistry();
        return "success";
    }

    private void refreshRegistry() {
        try {

            //获取真实DiscoveryClient对象实例
            DiscoveryClient discoveryClientProxy = (DiscoveryClient)getTarget(eurekaClientProxy);
            DiscoveryClient discoveryClient = (DiscoveryClient)getTarget(discoveryClientProxy);
            //刷新DiscoveryClient的serverList注册信息。注意，这儿没有刷新ribbon缓存
            Method method = DiscoveryClient.class.getDeclaredMethod("refreshRegistry");
            method.setAccessible(true);
            method.invoke(discoveryClient);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    /**下面在spring代理类实例中获取真实实例的反射工具方法*/
    // 通过反射操作获取对应的类
    public static Object getTarget(Object proxy) throws Exception {
        // 不是代理对象
        if (!AopUtils.isAopProxy(proxy)) {
            return proxy;
        }
        // 根据jdk进行反射操作
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            return getJdkDynamicProxyTargetObject(proxy);
        } else { // 根据cglib进行反射操作
            return getCglibProxyTargetObject(proxy);
        }
    }

    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        return target;
    }

    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport) advised.get(aopProxy)).getTargetSource().getTarget();
        return target;

    }


}
