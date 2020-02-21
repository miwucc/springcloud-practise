package com.neo.controller.interceptor;

import com.neo.controller.ribbonrule.RibbonUserIdHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/***
 * 如果全部都是用feign进行远程调用
 * 则用feign拦截器，把header的东西进行设置往后面的服务传
 * 如果用了hystrix，因为线程隔离机制，ThreadLocal则传递不了数值，RibbonUserIdHolder.get()就取不到东西了
 * 需要用阿里的TransmittableThreadLocal或者sleuth的HystrixRequestVariableDefault来进行传递
 */
@Component
public class FeginInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        Integer userId = RibbonUserIdHolder.get();
        requestTemplate.header("userId",String.valueOf(userId));
    }
}
