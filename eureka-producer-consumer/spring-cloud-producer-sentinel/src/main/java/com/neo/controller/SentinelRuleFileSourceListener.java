package com.neo.controller;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.FileRefreshableDataSource;
import com.alibaba.csp.sentinel.init.InitExecutor;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**启动的时候默认先从配置文件读取初始化的规则，然后才采用dashboard上的规则*/
@Component
public class SentinelRuleFileSourceListener {



    //规则解析器
    private Converter<String, List<FlowRule>> flowRuleListParser = source -> JSON.parseObject(source,
            new TypeReference<List<FlowRule>>() {});
    private Converter<String, List<DegradeRule>> degradeRuleListParser = source -> JSON.parseObject(source,
            new TypeReference<List<DegradeRule>>() {});
    private Converter<String, List<SystemRule>> systemRuleListParser = source -> JSON.parseObject(source,
            new TypeReference<List<SystemRule>>() {});

    //规则解析加载监听器
    private void listenRules() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();

        URL flowRuleURL = classLoader.getResource("sentinel-rule/FlowRule.json");
        URL degradeRule = classLoader.getResource("sentinel-rule/DegradeRule.json");
        URL systemRule = classLoader.getResource("sentinel-rule/SystemRule.json");

        if(flowRuleURL!=null){
            String flowRulePath = URLDecoder.decode(classLoader.getResource("sentinel-rule/FlowRule.json").getFile(), "UTF-8");
            // Data source for FlowRule
            FileRefreshableDataSource<List<FlowRule>> flowRuleDataSource = new FileRefreshableDataSource<>(
                    flowRulePath, flowRuleListParser);
            FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
        }

        if(degradeRule!=null){
            String degradeRulePath = URLDecoder.decode(classLoader.getResource("sentinel-rule/DegradeRule.json").getFile(), "UTF-8");

            // Data source for DegradeRule
            FileRefreshableDataSource<List<DegradeRule>> degradeRuleDataSource
                    = new FileRefreshableDataSource<>(
                    degradeRulePath, degradeRuleListParser);
            DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());
        }

        if(systemRule!=null){
            String systemRulePath = URLDecoder.decode(classLoader.getResource("sentinel-rule/SystemRule.json").getFile(), "UTF-8");
            // Data source for SystemRule
            FileRefreshableDataSource<List<SystemRule>> systemRuleDataSource
                    = new FileRefreshableDataSource<>(
                    systemRulePath, systemRuleListParser);
            SystemRuleManager.register2Property(systemRuleDataSource.getProperty());

        }

    }

    @PostConstruct
    void init(){
        try {
            listenRules();
            //默认触发连接dashboard，在资源端点被调用之前就主动上报信息
            InitExecutor.doInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


