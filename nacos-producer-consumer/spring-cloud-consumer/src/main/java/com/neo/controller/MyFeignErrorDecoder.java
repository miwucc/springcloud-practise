package com.neo.controller;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static java.lang.String.format;

/**
 * @Description 用自己定义的异常解码处理器替代原有的ErrorDecoder里面的default处理器
 * @Author xl
 * @Date 2019/10/16
 */
@Configuration
public class MyFeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        MyFeignException exception = errorStatus(methodKey, response);
        return exception;
    }


    public static MyFeignException errorStatus(String methodKey, Response response) {
        String message = format("status %s reading %s", response.status(), methodKey);
        String body=null;
        try {
            if (response.body() != null) {
                body = Util.toString(response.body().asReader());
                message += "; content:\n" + body;
            }
        } catch (IOException ignored) { // NOPMD
        }
        return new MyFeignException(response.status(), message,body);
    }



}
