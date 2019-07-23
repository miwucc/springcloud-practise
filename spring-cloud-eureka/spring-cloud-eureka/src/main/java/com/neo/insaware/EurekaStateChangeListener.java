package com.neo.insaware;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.lease.Lease;
import com.netflix.eureka.registry.AbstractInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.registry.ResponseCache;
import com.netflix.eureka.registry.ResponseCacheImpl;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 监听注册中心中的服务上下线事件，
 * 通知上下线服务类型以外的所有服务实时更新DiscoveryClient的Servcerlist同时刷新ribbon缓存
 * 这个解决方案的最大问题是，这里监听的上下线事件可能会收到多次，即使过滤掉复制请求，比如registered，好像
 * 不同状态(up和down)下都会register目前原因不明，down的时候会收到多次，且没有复制参数，不知道是不是bug
 * 那么可能收到多次事件进行多次通知，同时因为是http请求，如果需要通知的服务很多，则效率也不高，说不定一轮下来也有个
 * 7，8s了，可能服务客户端已经轮询拉到新列表了，也做不到秒级切换。
 * 如果要提高这个性能其实可以引入redis等消息监听订阅机制，但是这个又把注册中心搞得更耦合了。
 *
 * 另外，注冊上來的instanceId建议配置為:eureka.instance.instance-id=${spring.cloud.client.ipAddress}:${server.port}
 * 不然通知到各个服务发送的instanceId可能各种格式都有，不一定方便使用
 *
 * 所以建议，在服务注册列表<=100个的情况下，还是不使用实时通知机制，只想办法个各级缓存调到时间尽量小。
 * 让客户端对上下线的感知时间<=8s即可，如果全部采取默认配置，最大延时感知时间可能在240s
 *
 * eurekaServer 有3个级别的缓存，都在AbstractInstanceRegistry握有这些缓存的对象
 * 分别是 eurekaServer( registry -> writeReadConcurentMap -> readOnlyMap )- 被拉取->DiscoveryClient(serverList->ribbonCache)->被使用调用线程
 * 三方参考文章：https://mp.weixin.qq.com/s/zwoIDzX8WouYVrBfMJMpJQ
 *
 *
 *
 */
@Component
public class EurekaStateChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(EurekaStateChangeListener.class);

    /**默认的上下线通知请求地址*/
    String PROTCOL_PREFIX = "http://";
    String NOFITYUPURL_SUFFIX = "/notifyInsAware/up";
    String NOFITYDOWNURL_SUFFIX = "/notifyInsAware/cancel";

    /**LRU map，当满1000个时候根据LRU移除元素*/
    private ConcurrentLinkedHashMap<String, Long> cancelHistoryMap = new ConcurrentLinkedHashMap.Builder<String, Long>()
            //设置最大元素数量
            .maximumWeightedCapacity(1000).build();;


    /** http连接池,用于请求每个服务实例进行实时上下线通知, 最大连接数100，每个host(以上两个url会判定为同一个url)最大5，2秒验证返回超时时间*/
    private CloseableHttpClient httpClient = HttpUtils.createPoolClient(100, 5, 2 * 1000, 2 * 1000);

    /** 获取服务注册最实时列表的实际实现类对象*/
    @Autowired
    PeerAwareInstanceRegistry peerAwareInstanceRegistry;

    /**
     * 服务注册事件监听，收到后过滤掉eurekaServer之间的复制传播信息，过滤掉其它status非UP的注册，只传播UP的注册通知
     * @param event
     */
    @EventListener
            /**过滤掉复制信息*/
            (condition = "#event.replication==false")
    public void listenUp(EurekaInstanceRegisteredEvent event) {

        if(!event.isReplication()){
            logger.info(MarkerFactory.getMarker("UP"), event.getInstanceInfo().getAppName() + " 服务注册EVENT收到!");
        }

        logger.info("EurekaInstanceRegisteredEvent 收到! 当前注册实例：" + event.getInstanceInfo().getAppName() + "," + event.getInstanceInfo().getInstanceId());

        //只传播状态为UP的注册时间进行通知
        if(event.getInstanceInfo().getStatus()==null
                ||event.getInstanceInfo().getStatus().name()==null
                ||!"UP".equals(event.getInstanceInfo().getStatus().name().toUpperCase())){
            return;
        }

        //刷新readOnly缓存
        refreshReadOnlyCache();
        //缓存刷新完后进行其它服务列表成员通知
        notifyAllOtherService(event);

    }

    /**
     * 服务下线事件
     *
     * @param event
     */
    @EventListener
            (condition = "#event.replication==false")
    public void listenCancel(EurekaInstanceCanceledEvent event){

        if(!event.isReplication()){
            logger.info(MarkerFactory.getMarker("CANCEL"), event.getAppName() + " 服务下线EVENT收到!");
        }

        logger.info("EurekaInstanceCanceledEvent 收到! 当前下线实例：" + event.getAppName() + "," + event.getServerId());

        //这儿因为EurekaInstanceCanceledEvent 没有区分是否Replication，所以会收到多次事件，为了解决这个问题可以自己写一个1秒内的通知记录维护列表，如果1秒内收到多个同样instanceId的则不再重复发送
        Long lastTimeSecMs = cancelHistoryMap.get(event.getServerId());
        //如果1秒内同一个实例通知过一次，则不做2次通知
        if(lastTimeSecMs!=null&&(System.currentTimeMillis()-lastTimeSecMs.longValue()<1000)){
            logger.info("EurekaInstanceCanceledEvent 收到! 当前下线实例：" + event.getAppName() + "," + event.getServerId()+",因为1s内做过一次通知因此不再做通知");
            return;
        }

        //刷新readOnly缓存
        refreshReadOnlyCache();
        //缓存刷新完后进行其它服务列表成员通知
        notifyAllOtherService(event);
        //cancel的时候,通知完了记录一次最近通知时间
        cancelHistoryMap.put(event.getServerId(),System.currentTimeMillis());
    }



    /** 缓存刷新完后进行其它服务列表成员通知 */
    private void notifyAllOtherService(ApplicationEvent event){
        //服务名
        String eventAppName=null;
        //注册事件
        String eventInstanceId=null;
        String eventUpOrDown=null;
        String notifyURLSuffix=null;
        //注册事件
        if(event instanceof EurekaInstanceRegisteredEvent){
            EurekaInstanceRegisteredEvent eurekaInstanceRegisteredEvent =  (EurekaInstanceRegisteredEvent)event;
            eventAppName=eurekaInstanceRegisteredEvent.getInstanceInfo().getAppName();
            eventInstanceId=eurekaInstanceRegisteredEvent.getInstanceInfo().getInstanceId();
            eventUpOrDown="UP";
            notifyURLSuffix = NOFITYUPURL_SUFFIX;
            //下线事件
        }else if(event instanceof EurekaInstanceCanceledEvent){
            EurekaInstanceCanceledEvent eurekaInstanceCanceledEvent =  (EurekaInstanceCanceledEvent)event;
            eventAppName=eurekaInstanceCanceledEvent.getAppName();
            eventInstanceId=eurekaInstanceCanceledEvent.getServerId();
            eventUpOrDown="CANCEL";
            notifyURLSuffix = NOFITYDOWNURL_SUFFIX;
        }

        if(eventAppName==null||eventInstanceId==null){
            return;
        }

        try {
            //遍历当前实际注册服务，一个个通知当前服务发生改变
            Field registryFiled = AbstractInstanceRegistry.class.getDeclaredField("registry");
            registryFiled.setAccessible(true);
            ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>> registry = (ConcurrentHashMap<String, Map<String, Lease<InstanceInfo>>>) registryFiled.get(peerAwareInstanceRegistry);

            //遍历最准确的服务列表持有者registry，该持有者就是UI后台界面的数据来源
            for (Map<String, Lease<InstanceInfo>> leaseMap : registry.values()) {
                for (Map.Entry<String, Lease<InstanceInfo>> leaseEntry : leaseMap.entrySet()) {
                    Lease<InstanceInfo> lease = leaseEntry.getValue();
                    if (lease.getHolder() != null) {
                        InstanceInfo holdInstanceInfo = (InstanceInfo) lease.getHolder();
                        if (holdInstanceInfo != null) {
                            boolean isSameApp = eventAppName.equals(holdInstanceInfo.getAppName());
                            //如果是相同的服务，则不通知更新，只对其它服务进行更新通知
                            if (!isSameApp) {
                                String notifyURL = PROTCOL_PREFIX + holdInstanceInfo.getIPAddr() + ":" + holdInstanceInfo.getPort() + notifyURLSuffix;
                                // 4.发送HTTP请求，这里使用的是HttpClient工具包，产品可自行选择自己熟悉的工具包发送请求
                                Map<String, String> headers = new HashMap<String, String>();
                                Map<String, String> params = new HashMap<String, String>();
                                params.put("appName", eventAppName);
                                params.put("instanceId", eventInstanceId);
                                int httpStatusCode=0;
                                try {
                                    HttpResponse httpResponse = HttpUtils.doGet(httpClient, notifyURL, "", headers, params);
                                    // 返回状态码
                                    httpStatusCode = httpResponse.getStatusLine().getStatusCode();
                                    if(httpStatusCode%100==2){
                                        logger.info("服务appName="+eventAppName+",instanceId="+eventInstanceId+" ,eventType="+eventUpOrDown+" 通知成功！notify="+notifyURL+" httpStatusCode=" + httpStatusCode);
                                    }

                                } catch (Exception e) {
                                    logger.info("服务appName="+eventAppName+",instanceId="+eventInstanceId+" ,eventType="+eventUpOrDown+" 通知失败！ notify="+notifyURL+" httpStatusCode=" + httpStatusCode,e);

                                } finally {

                                }

                            }

                        }
                    }
                }
            }

        }catch (NoSuchFieldException e){
            logger.error("notifyAllOtherService失败",e);

        }catch (IllegalAccessException e){
            logger.error("notifyAllOtherService失败",e);
        }

    }


    private void refreshReadOnlyCache() {
        Field responseCacheFiled = null;
        try {
            //获取readOnlyMap级别缓存持有这对象，并进行刷新
            responseCacheFiled = AbstractInstanceRegistry.class.getDeclaredField("responseCache");
            responseCacheFiled.setAccessible(true);
            ResponseCache responseCacheImpl =  (ResponseCache)ReflectionUtils.getField(responseCacheFiled,peerAwareInstanceRegistry);

            //responseCache缓存在第一次注册之后才会生成,这之前不要调用
            if(responseCacheImpl!=null) {
                Field shouldUseReadOnlyResponseCacheField = ResponseCacheImpl.class.getDeclaredField("responseCache");
                shouldUseReadOnlyResponseCacheField.setAccessible(true);
                boolean shouldUseReadOnlyResponseCache = shouldUseReadOnlyResponseCacheField.getBoolean(responseCacheImpl);

                //判断是否握有配置了使用readOnlyCache,如果有，则进行刷新该级别缓存
                if (shouldUseReadOnlyResponseCache) {
                    Method getCacheUpdateTaskMethod = ResponseCacheImpl.class.getDeclaredMethod("getCacheUpdateTask");
                    getCacheUpdateTaskMethod.setAccessible(true);
                    //立即刷新缓存
                    TimerTask task = (TimerTask) getCacheUpdateTaskMethod.invoke(responseCacheImpl);
                    task.run();
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
    }


}
