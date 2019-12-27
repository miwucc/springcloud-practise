package com.neo.remote;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by summer on 2017/5/15.
 */
@Component
public class HelloRemoteFallback implements HelloRemote{

    @Override
    public String hello(@RequestParam(value = "name") String name) {
        return "hello " +name+", this messge send failed ";
    }

    //这里无法对异常进行细致捕捉，只能直接fallback
    @Override
    public String helloHystrixTest(String name) {
        System.out.println("helloHystrixTest is fallback!!");
        return "helloHystrixTest is fallback!!";
    }
}
