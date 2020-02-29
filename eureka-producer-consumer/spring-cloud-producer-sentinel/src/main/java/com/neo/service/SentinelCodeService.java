package com.neo.service;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**此方法展示硬编码方式展示限流作用*/
@Service
public class SentinelCodeService {

/** Sentinel 支持以下几种规则：
// 流量控制规则：FlowRule
// 熔断降级规则：DegradeRule
// 系统保护规则：SystemRule
// 来源访问控制规则：AuthorityRule
// 热点参数规则：ParamFlowRule
 */

/**
 * 判断限流降级异常
 * 在 Sentinel 中所有流控降级相关的异常都是异常类 BlockException 的子类：
 * 流控异常：FlowException
 * 熔断降级异常：DegradeException
 * 系统保护异常：SystemBlockException
 * 热点参数限流异常：ParamFlowException
 * 我们可以通过以下函数判断是否为 Sentinel 的流控降级异常：
 * BlockException.isBlockException(Throwable t);
 * */

    private void initFlowRules(){

        // 定义热点限流的规则，对第一个参数设置 qps 限流模式，阈值为5
        FlowRule rule = new FlowRule();
        rule.setResource(HELLO_CODE_SOURCE);
        // 限流类型，qps
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 设置阈值
        rule.setCount(5);
        // 限制哪个调用方
        rule.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);
        // 基于调用关系的流量控制
        rule.setStrategy(RuleConstant.STRATEGY_DIRECT);
        // 流控策略
        rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        FlowRuleManager.loadRules(Collections.singletonList(rule));
    }

    // 定义的资源
    public static final String HELLO_CODE_SOURCE = "sentinelHelloCodeSource";

    @PostConstruct
    void init(){
        initFlowRules();
    }


    public String helloCodeSource(){
        // 1.5.0 版本开始可以直接利用 try-with-resources 特性，自动 exit entry
        // 创建一个resource 名为HelloWorld
        Entry entry=null;
        try {
            entry = SphU.entry(HELLO_CODE_SOURCE);
            // 被保护的逻辑
            System.out.println("OK hello world helloCodeSource");
            return "Hello! this is "+HELLO_CODE_SOURCE+" time="+System.currentTimeMillis();
        } catch (BlockException ex) {
            // 处理被流控的逻辑
            System.out.println("blocked helloCodeSource! Time="+System.currentTimeMillis());
            return "blocked helloCodeSource "+HELLO_CODE_SOURCE+" time="+System.currentTimeMillis();
            //// 1.5.0 版本开始可以直接利用 try-with-resources 特性，自动 exit entry 可以不要finally
        }finally {
            if (entry != null) {
                entry.exit();
            }
        }

    }

    public static void main(String[] args) {

        //main方法测试是不会打印日志的。。。

        SentinelCodeService sentinelCodeService = new SentinelCodeService();
        sentinelCodeService.initFlowRules();

        for(int i=0;i<200;i++){
            sentinelCodeService.helloCodeSource();
        }

    }
}
