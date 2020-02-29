package com.neo.controller;//package com.neo.controller;

import com.alibaba.csp.sentinel.init.InitExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 不用触发资源调用的时候才连接dashboard，直接启动就连接
 */
@Service
public class SentinelAutoLinkDashboard {
    @PostConstruct
    void init(){
        try {
            //默认触发连接dashboard，在资源端点被调用之前就主动上报信息
            InitExecutor.doInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
