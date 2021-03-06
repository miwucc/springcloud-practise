package com.neo.remote;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by summer on 2017/5/11.
 */
@FeignClient(name= "spring-cloud-producer")
public interface HelloRemote {


    @RequestMapping(value = "/test-gzip")
    public String testGzip();

    @RequestMapping(value = "/hello")
    public String hello(@RequestParam(value = "name") String name);

    @RequestMapping(value = "/throwExp")
    public String throwExp(@RequestParam(value = "name") String name);

    @RequestMapping(value = "/noResponse")
    public void noResponse(@RequestParam(value = "name") String name);


}
