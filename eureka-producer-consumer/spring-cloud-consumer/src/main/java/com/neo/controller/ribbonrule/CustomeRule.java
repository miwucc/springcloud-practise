package com.neo.controller.ribbonrule;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import java.util.List;

//自定义一个负载均衡策略，实现IRule接口，这里是继承的最顶端的抽象类
public class CustomeRule extends AbstractLoadBalancerRule
{

    private int total = 0;             // 总共被调用的次数，目前要求每台被调用5次
    private int currentIndex = 0;    // 当前提供服务的机器号

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {

    }

    //选择哪台机器
    @Override
    public Server choose(Object key) {
        //父类方法去除当前loadBalancer
        ILoadBalancer lb = getLoadBalancer();

        if(lb==null){
            return null;
        }

        Server server = null;

        while (server == null) {
            if (Thread.interrupted()) {
                return null;
            }

            Integer userId = RibbonUserIdHolder.get();

            List<Server> upList = lb.getReachableServers(); //当前存活的服务，如果不是UP状态，或者被从eureka中心delete，ribbon节点感应到后，会从ReachableList里面移除掉
            List<Server> allList = lb.getAllServers();  //获取全部的服务

            int serverCount = allList.size();
            if (serverCount == 0) {
                return null;
            }

            int upServerCount = upList.size();
            if (upServerCount == 0) {
                return null;
            }

            //如果没有用户ID，则采用每个服务器轮询5次的方式
            if(userId==null){
                //int index = rand.nextInt(serverCount);
                //server = upList.get(index);
                if(total < 5)
                {
                    server = upList.get(currentIndex);
                    total++;
                }else {
                    total = 0;
                    currentIndex++;
                    if(currentIndex >= upList.size())
                    {
                        currentIndex = 0;
                    }
                }
            //如果有用户ID，则采用用户ID取当前可用服务器取模的方式
            }else{
                if(upList!=null){
                    for(Server s1 : upList){
                        System.out.println("id="+s1.getId()+",alive="+s1.isAlive());
                    }
    //                Integer idx = userId%upServerCount;
    //                server = upList.get(idx);

                    //方便测试改为指定，用来模拟9900这个实力pause变为down状态后会切换为使用另一个实例
                    server = upList.get(0);
                    if(!server.getId().equals("192.168.17.1:9900")){
                        if(upList.size()>1){
                            server = upList.get(1);
                        }
                    }

                }

            }


            if (server == null) {
                Thread.yield();
                continue;
            }

            if (server.isAlive()) {
                return (server);
            }

            // Shouldn't actually happen.. but must be transient or a bug.
            server = null;
            Thread.yield();
        }
        //用完后记得移除，以免发生内存泄漏
        RibbonUserIdHolder.remove();
        return server;
    }
}
