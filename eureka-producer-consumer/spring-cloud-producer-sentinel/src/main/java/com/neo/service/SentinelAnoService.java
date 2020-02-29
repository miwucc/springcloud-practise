package com.neo.service;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.init.InitExecutor;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class SentinelAnoService {

//    // 对应的 `handleException` 函数需要位于 `ExceptionUtil` 类中，并且必须为 static 函数.
//    @SentinelResource(value = "test", blockHandler = "handleException", blockHandlerClass = {ExceptionUtil.class})
//    public void test() {
//        System.out.println("Test");
//    }

    //先定义几个默认的流量规则在这儿创建
//    @PostConstruct
//    void initRules(){

//        //默认触发连接dashboard，在资源端点被调用之前就主动上报信息
//        InitExecutor.doInit();
//
//        //限流规则，不知道为啥aop注解的规则这么加不进去，现在全部改为dataSource json文件的形式载入
//        List<FlowRule> flowRules = new ArrayList<>();
//        FlowRule rule = new FlowRule("hello");
//        // set limit qps to 20
//        rule.setCount(1);
//        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
//        rule.setLimitApp("default");
//        flowRules.add(rule);
//        FlowRuleManager.loadRules(flowRules);

//    Sentinel 熔断降级支持以下几种策略：
//

//    秒级 RT 模式：若持续进入 5 个请求，它们资源的平均响应时间都超过阈值（秒级平均 RT，以 ms 为单位），资源调用会被熔断。在接下的降级时间窗口（在降级规则中配置，以 s 为单位）之内，对这个方法的调用都会自动地返回（抛出 DegradeException）。
//    秒级异常比例模式：当资源的每秒异常数占通过量的比值超过阈值之后，资源进入降级状态，即在接下的降级时间窗口（在降级规则中配置，以 s 为单位））之内，对这个方法的调用都会自动地返回。异常比率的阈值范围是 [0.0, 1.0]，代表 0% - 100%。
//    分钟级异常数模式：当资源最近 1 分钟的异常数目超过阈值之后会进行熔断。

    //    !!!!!! 为什么RT和比率模式都是秒级的统计窗口？异常数模式固定是分钟级的？ 不能设置统计窗口时长，只能设置熔断持续时间窗口时长？
    //    。。。。难道是为了监控性能日志是秒级的方便打印?那流量小的业务咋办，1S内少于5个请求，的意思是说这系统qps太低没必要熔断了？
    //    所以这儿的理解非常重要！！！！熔断保护的是什么！！，是异常情况下对占用等待线程池的堆积！所以qps很低的情况下没有必要开熔断，触发异常直接走fallback方法就完了。
    //    比如qps<5,比如10秒内4个请求，这种情况下你失败就失败，用不着触发熔断，顶多10s堆积浪费你4个线程！


//        //降级规则
//        List<DegradeRule> degradeRules = new ArrayList<>();
//        DegradeRule degradeRule = new DegradeRule();
//        degradeRule.setResource("hello");
//        // set threshold RT, 10 ms
//        degradeRule.setCount(10);//10ms,意义为平均相应RT时间超过这个值，则促发降级
//        degradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);//降级规则，点进去可以看又5种，这种是根据RT时间ms来判断的
//        degradeRule.setTimeWindow(10);//降级持续时间
////        degradeRule.setRtSlowRequestAmount(5);//RT 模式下 连续多少个请求的平均 RT 超出阈值方可触发熔断
////        degradeRule.setMinRequestAmount(5);//异常熔断的触发最小请求数，请求数小于该值时即使异常比率超出阈值也不会熔断（1.7.0 引入）
//        degradeRules.add(degradeRule);
//        DegradeRuleManager.loadRules(degradeRules);
//
//        //系统保护规则 (SystemRule)，值对EntryType.IN的类型生效
//        List<SystemRule> systemRules = new ArrayList<>();
//        SystemRule systemRule = new SystemRule();
////        systemRule.setResource("hello");
//        systemRule.setHighestSystemLoad(10);
//        systemRules.add(systemRule);
//        SystemRuleManager.loadRules(systemRules);

        //访问控制规则 (AuthorityRule)

//    }

    // 注意！！！！连接doashboard控制台，是需要触发一次entry资源的调用，然后触发Env static InitExecutor.doInit();才能连接上doashboad
    // 采用注解的时候，必须要确保aop生效了，有aop成entry，不然也触发不了上的方法，dashboard里面看不到！！！
    // 启动的时候也可以先先手动调用一次InitExecutor.doInit()，确保没端点被调用的时候也生效
    @SentinelResource(value = "hello",
//            entryType = EntryType.IN, //是入口流量（EntryType.IN）还是出口流量（EntryType.OUT），注意系统规则只对 IN 生效
            blockHandler = "exceptionHandler",
            fallback = "helloFallback",
            exceptionsToIgnore = {IllegalStateException.class})
    public String hello(long s) {
        if(s<0){
            throw new IllegalStateException("fei fa s!!!");
        }
        return String.format("Hello at %d", s);
    }

//    PS：这里有个需要注意的知识点，就是 SphU.entry 方法的第二个参数 EntryType 说的是这次请求的流量类型，共有两种类型：IN 和 OUT 。
//
//    IN：是指进入我们系统的入口流量，比如 http 请求或者是其他的 rpc 之类的请求。
//
//    OUT：是指我们系统调用其他第三方服务的出口流量。
//
//    入口、出口流量只有在配置了系统规则时才有效。
//
//    设置 Type 为 IN 是为了统计整个系统的流量水平，防止系统被打垮，用以自我保护的一种方式。

//    设置 Type 为 OUT 一方面是为了保护第三方系统，比如我们系统依赖了一个生成订单号的接口，而这个接口是核心服务，如果我们的服务是非核心应用的话需要对他进行限流保护；另一方面也可以保护自己的系统，假设我们的服务是核心应用，而依赖的第三方应用老是超时，那这时可以通过设置依赖的服务的 rt 来进行降级，这样就不至于让第三方服务把我们的系统拖垮。

    //熔断降级触发
    // Fallback 函数，函数签名与原函数一致或加一个 Throwable 类型的参数.
    public String helloFallback(long s,Throwable ex) {

        return String.format("fallback %d", s);
    }

    //注意 blockHandler 函数会在原方法被限流/降级/系统保护的时候调用，而 fallback 函数会针对所有类型的异常。
    //限流阻塞触发
    // Block 异常处理函数，参数最后多一个 BlockException，其余与原函数一致.
    public String exceptionHandler(long s, BlockException ex) {
        // Do some log here.
        ex.printStackTrace();
        return "block, error occurred at " + s;
    }


//    使用方法
//    注意：注解方式埋点不支持 private 方法。
//
//    @SentinelResource 用于定义资源，并提供可选的异常处理和 fallback 配置项。 @SentinelResource 注解包含以下属性：
//
//    value：资源名称，必需项（不能为空）
//    entryType：entry 类型，可选项（默认为 EntryType.OUT）
//    blockHandler / blockHandlerClass: blockHandler 对应处理 BlockException 的函数名称，可选项。blockHandler 函数访问范围需要是 public，返回类型需要与原方法相匹配，参数类型需要和原方法相匹配并且最后加一个额外的参数，类型为 BlockException。blockHandler 函数默认需要和原方法在同一个类中。若希望使用其他类的函数，则可以指定 blockHandlerClass 为对应的类的 Class 对象，注意对应的函数必需为 static 函数，否则无法解析。
//    fallback：fallback 函数名称，可选项，用于在抛出异常的时候提供 fallback 处理逻辑。fallback 函数可以针对所有类型的异常（除了 exceptionsToIgnore 里面排除掉的异常类型）进行处理。fallback 函数签名和位置要求：
//    返回值类型必须与原函数返回值类型一致；
//    方法参数列表需要和原函数一致，或者可以额外多一个 Throwable 类型的参数用于接收对应的异常。
//    fallback 函数默认需要和原方法在同一个类中。若希望使用其他类的函数，则可以指定 fallbackClass 为对应的类的 Class 对象，注意对应的函数必需为 static 函数，否则无法解析。
//    defaultFallback（since 1.6.0）：默认的 fallback 函数名称，可选项，通常用于通用的 fallback 逻辑（即可以用于很多服务或方法）。默认 fallback 函数可以针对所有类型的异常（除了 exceptionsToIgnore 里面排除掉的异常类型）进行处理。若同时配置了 fallback 和 defaultFallback，则只有 fallback 会生效。defaultFallback 函数签名要求：
//    返回值类型必须与原函数返回值类型一致；
//    方法参数列表需要为空，或者可以额外多一个 Throwable 类型的参数用于接收对应的异常。
//    defaultFallback 函数默认需要和原方法在同一个类中。若希望使用其他类的函数，则可以指定 fallbackClass 为对应的类的 Class 对象，注意对应的函数必需为 static 函数，否则无法解析。
//    exceptionsToIgnore（since 1.6.0）：用于指定哪些异常被排除掉，不会计入异常统计中，也不会进入 fallback 逻辑中，而是会原样抛出。
//    注：1.6.0 之前的版本 fallback 函数只针对降级异常（DegradeException）进行处理，不能针对业务异常进行处理。
//
//    特别地，若 blockHandler 和 fallback 都进行了配置，则被限流降级而抛出 BlockException 时只会进入 blockHandler 处理逻辑。若未配置 blockHandler、fallback 和 defaultFallback，则被限流降级时会将 BlockException 直接抛出（若方法本身未定义 throws BlockException 则会被 JVM 包装一层 UndeclaredThrowableException）。
}
