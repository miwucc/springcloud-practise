package com.neo.remote;

import com.neo.controller.MyFeignErrorDecoder2;
import feign.codec.ErrorDecoder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description 自定义配置，用来替换默认生成的feignClient里面的一些bean对象，
 * 比如{@link feign.codec.Decoder}, {@link feign.codec.Encoder}, {@link feign.Contract}
 * @Author xl
 * @Date 2020/1/3
 */
//直接在这儿写就可以对全局的feign进行替换，如果不想全局替换的话就要写到@FeignClient的注解里面去
@Configuration
public class CustomerFeignConfiguration {

    //替换异常处理解码器对象
    @Bean
    public ErrorDecoder errorDecoder() {
        return new MyFeignErrorDecoder2();
    }

//    @Bean
//    public ErrorDecoder errorDecoder() {
//        return new MyFeignErrorDecoder();
//    }

}
