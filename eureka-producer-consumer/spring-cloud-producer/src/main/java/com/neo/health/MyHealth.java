package com.neo.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @Description 自定义一个健康检查项
 * @Author xl
 * @Date 2019/9/26
 */
@Component("Myhealth")
public class MyHealth implements HealthIndicator {

    public static final int GOOD = 0;
    public static final int DOWN = 0;

    public static int healthCode = GOOD;//健康状态

    public static int getHealthCode() {
        return healthCode;
    }

    public static void setHealthCode(int healthCode) {
        MyHealth.healthCode = healthCode;
    }

    @Override
    public Health health() {

        // perform some specific health check
        if (healthCode != GOOD) {
            return Health.down().withDetail("Myhealth Error Code1", healthCode+",hehehe").build();
        }
        return Health.up().withDetail("Myhealth Error Code2","all is ok").build();

    }

    @RestController
    public class MyHealthController {

        @RequestMapping(value = "/set-myhealth/{data}", method = RequestMethod.GET)
        public String unhealth(@PathVariable String data) {

            if(data!=null){
                healthCode = Integer.valueOf(data);
                return "set "+data+" success!";
            }else{
                return "fail data is null";
            }

        }

    }
}
