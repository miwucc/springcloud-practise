package com.neo.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * @Description 自定义一个健康检查项
 * @Author xl
 * @Date 2019/9/26
 */
@Component("Myhealth")
public class MyHealth implements HealthIndicator {
    @Override
    public Health health() {

        // perform some specific health check
        int errorCode = 0;
        if (errorCode != 0) {
            return Health.down().withDetail("Myhealth Error Code1", errorCode+",hehehe").build();
        }
        return Health.up().withDetail("Myhealth Error Code2","all is ok").build();

    }
}
