package com.neo.remote;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**使用feginhttp-client后，默认的是HttpClientBuilder.create().build()，里面的线程池数量并没有设置,这里设置一个定制的连接池再返回*/
@Configuration
public class FeginHttpClientConfiguration {

    @Bean(destroyMethod = "close")
    public PoolingHttpClientConnectionManager poolingHttpClientConnectionManager(){
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(100);
        connectionManager.setMaxTotal(200);
        return connectionManager;
    }

    @Bean
    public HttpClientBuilder httpClientBuilder(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager){
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(3000)//创建连接超时时间
                .setSocketTimeout(15000)//数据返回超时时间
                .setConnectionRequestTimeout(2000)//从连接池取连接超时时间
                .build();
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionManager(poolingHttpClientConnectionManager);
        builder.setDefaultRequestConfig(requestConfig);
        //因为本身ribbon以后重试，这里就不要再搞重试了
        builder.setRetryHandler(new DefaultHttpRequestRetryHandler(0,false));
        return builder;
    }

    @Bean
    @Primary
    public HttpClient httpClient(HttpClientBuilder httpClientBuilder){
        return httpClientBuilder.create().build();
    }

}
