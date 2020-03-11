package com.neo.remote;

import com.neo.remote.feigngzip.FeignContentGzipEncodingInterceptor;
import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.cloud.netflix.feign.encoding.FeignClientEncodingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/***
 * 参考文献： https://blog.csdn.net/xjune/article/details/80198850
 * 源码中因为和ribbon一起使用的时候 ApacheHttpClient不会生成，所以源码的FeignContentGzipEncodingAutoConfiguration不会生效，这里自己弄一个一样的来生效
 *
 *  * 其实并没有什么卵用，只是根据request的Content-Type和Content-Length，在header上加上content-encoding=gzip，标识我请求过来的是gzip压缩信息，你需要解压。
 *  * 拦截器本身不负责压缩。
 *  * 这个其实是要不得的，Content-Type和Content-Length一般用在response里面而非request里面，除非自己定制。而且需要自己找地方进行内容压缩。
 *  所以建议还是不要配，配了反倒容易出问题
 *  网上都是瞎扯淡，没有看源码就喊配置
 * feign.compression.request.enabled=true
 * feign.compression.request.mime-types=text/xml,application/xml,application/json
 * feign.compression.request.min-request-size=1
 */
@EnableConfigurationProperties(FeignClientEncodingProperties.class)
@ConditionalOnClass(Feign.class)
//@ConditionalOnBean(ApacheHttpClient.class)
@ConditionalOnProperty(value = "feign.compression.request.enabled", matchIfMissing = false)
@AutoConfigureAfter(FeignAutoConfiguration.class)
@Configuration
public class FeignContentGzipEncodingAutoConfiguration {

    @Bean
    public FeignContentGzipEncodingInterceptor feignContentGzipEncodingInterceptor(FeignClientEncodingProperties properties) {
        return new FeignContentGzipEncodingInterceptor(properties);
    }

}
