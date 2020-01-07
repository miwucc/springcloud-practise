package com.neo.controller;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * @Description 用自己定义的http非200-300异常解码处理器替代原有的ErrorDecoder里面的default处理器
 * 把其中的4xx状态包装为HystrixBadRequestException异常，这样就抛出就可以不计入熔断器统计了
 * @Author xl
 * @Date 2019/10/16
 */
//@Configuration//这种写法和定义一个方法bean是一个意思
public class MyFeignErrorDecoder2 extends ErrorDecoder.Default {

    @Override
    public Exception decode(String methodKey, Response response) {

        int httpStatus = response.status();
        //处理异常为4xx的时候，不进入熔断计数,抛出HystrixBadRequestException异常
        //异常处理源码在AbstractCommand#executeCommandAndObserve --> handleFallback
        //开启404decode的时候不会走这里，404就会直接用404decode返回一个空值回去，类似于fallback，代码位置feign.SynchronousMethodHandler.executeAndDecode
        if(httpStatus==404
                ||httpStatus==400 //无效请求,说明服务器无法理解用户的请求，除非进行修改
                ||httpStatus==405 //资源被禁止，有可能是文件目录权限不够导致的
                ||httpStatus==403 //出现403是因为服务器拒绝了你的地址请求，很有可能是你根本就没权限访问网站
                ||httpStatus==409 // Web 服务器认为，由于与一些已经确立的规则相冲突， 客户端（如您的浏览器或我们的 CheckUpDown 机器人）提交的请求无法完成
                ||(httpStatus>=400&&httpStatus<500)//4xx状态全部不能进入熔断，这里需要做处理
                ){

            return new HystrixBadRequestException("4XX HTTP STATUS DECODE!" + response.toString());

        }
        //http状态为3xx的时候会自动重定向到不了这里
        //如果是5xx或其它非>=200&<300状态则启用默认解码器的方式构建FeignException或者RetryableException
        return super.decode(methodKey,response);

    }

}
