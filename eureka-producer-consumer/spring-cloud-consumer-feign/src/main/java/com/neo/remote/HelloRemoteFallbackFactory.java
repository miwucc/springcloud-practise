
package com.neo.remote;

import feign.hystrix.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 使用fallback工厂，可以捕捉到导致fallback的exception，进行打印，这样方便后面排查问题
 */
@Component
public class HelloRemoteFallbackFactory implements FallbackFactory<HelloRemote> {

    private static final Logger logger = LoggerFactory.getLogger(HelloRemoteFallbackFactory.class);

    @Override
    public HelloRemote create(Throwable cause) {
        logger.error("remote function is fallback ",cause);

        //在捕捉到异常后再考虑如何进行callback处理
        return new HelloRemote() {

            @Override
            public String hello(String name) {
                return "this is fallback hello!";
            }

            @Override
            public String helloHystrixTest(String name) {
                throw new RuntimeException("fallback的时候抛出的本地业务异常");
            }

            @Override
            public String helloHystrixTestRedirect(String name) {
                throw new RuntimeException("fallback的时候抛出的本地业务异常");
            }
        };
    }
}
