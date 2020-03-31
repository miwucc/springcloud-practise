package com.neo.controller;

import com.alibaba.fastjson.JSONObject;
import feign.FeignException;

/**
 * @Description 因为FeignException里面只有字符串，无法取到原始的http异常对象信息，所以这里自己封装一下
 * @Author xl
 * @Date 2019/10/16
 */
public class MyFeignException extends FeignException {

    ExceptionContent exceptionContent;

    protected MyFeignException(String message, Throwable cause) {
        super(message, cause);
    }

    protected MyFeignException(String message) {
        super(message);
    }

    protected MyFeignException(int status, String message,String bodyString) {
        super(status, message);
        if(bodyString!=null&&!"".equals(bodyString.trim())){
            this.exceptionContent = JSONObject.parseObject(bodyString,ExceptionContent.class);
        }else{
            this.exceptionContent = new ExceptionContent();
        }
    }

    public ExceptionContent getExceptionContent() {
        return exceptionContent;
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
