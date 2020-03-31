package com.neo.controller;

import com.alibaba.fastjson.JSONObject;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;

import java.io.IOException;

import static java.lang.String.format;

/**
 * @Description 用自己定义的异常解码处理器替代原有的ErrorDecoder里面的default处理器
 *
 * feign.SynchronousMethodHandler#executeAndDecode方法中写明了，在执行方法的时候。当返回的http状态不是2xx，且不是404decode
 * 的情况下走ErrorDecoder，默认的ErrorDecoder是feign.codec.ErrorDecoder.Default，他回抛出异常并且计算到断路器统计里面
 * 这里如果在开启了404decoder但是堆40x其它状态或者什么状态不能计入断路器计数的，则这里需要自己搞过滤
 * feign的默认配置类是FeignClientsConfiguration，
 * 这里是用ErrorDecoder自己的实例直接覆盖原有实例
 *
 * ErrorDecoder是用于解码在远端调用有正常http协议返回，但非200X且40x没有开启404decode的时候处理的情况，30x这种重定向会在前面就处理了，到不到这儿来，到这儿来的都是重定向请求后的
 * 源码在feign.SynchronousMethodHandler#executeAndDecode
 * 而不是说所有的异常都会到这里来解码
 * @Author xl
 * @Date 2019/10/16
 */
//@Configuration//这种写法和定义一个方法bean是一个意思
//public class MyFeignErrorDecoder implements ErrorDecoder {

public class MyFeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        Exception exception = errorStatus(methodKey, response);
        return exception;
    }

    /**
     * 这个地方的response是feign.SynchronousMethodHandler#executeAndDecode执行后的返回结果，是远端HTTP状态的结果的包装
     * 如果是本机的异常，超时异常，重试异常等等，不会走到这个解码器来，会直接进行fallback或者抛出
     * @param methodKey
     * @param response
     * @return
     */
    public static Exception errorStatus(String methodKey, Response response) {
        ExceptionContent exceptionContent=null;
        String message = format("status %s reading %s", response.status(), methodKey);
        String bodyString=null;
        try {
            if (response.body() != null) {
                bodyString = Util.toString(response.body().asReader());
                message += "; content:\n" + bodyString;
                if(bodyString!=null&&!"".equals(bodyString.trim())){
                    exceptionContent = JSONObject.parseObject(bodyString,ExceptionContent.class);
                }else{
                    exceptionContent = new ExceptionContent();
                }

                //处理异常为4xx的时候，不进入熔断计数
                //异常处理源码在AbstractCommand#executeCommandAndObserve --> handleFallback
                if(exceptionContent.getStatus()==404
                        ||exceptionContent.getStatus()==400 //无效请求,说明服务器无法理解用户的请求，除非进行修改
                        ||exceptionContent.getStatus()==405 //资源被禁止，有可能是文件目录权限不够导致的
                        ||exceptionContent.getStatus()==403 //出现403是因为服务器拒绝了你的地址请求，很有可能是你根本就没权限访问网站
                        ||exceptionContent.getStatus()==409 // Web 服务器认为，由于与一些已经确立的规则相冲突， 客户端（如您的浏览器或我们的 CheckUpDown 机器人）提交的请求无法完成
                        ||(exceptionContent.getStatus()>=400&&exceptionContent.getStatus()<500)//4xx状态全部不能进入熔断，这里需要做处理
                        ){
                    return new HystrixBadRequestException(message);
                }
            }
        } catch (IOException ignored) { // NOPMD
//            logger.error(ex.getMessage(), ex);
        }

        return new RuntimeException(message);
    }


    static class ExceptionContent{
        long timestamp;
        int status;
        String error;
        String exception;
        String message;
        String path;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getException() {
            return exception;
        }

        public void setException(String exception) {
            this.exception = exception;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }



}
