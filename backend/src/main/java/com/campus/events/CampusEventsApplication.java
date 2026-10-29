package com.campus.events;

import com.campus.events.config.AppProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.campus.events")
@EnableConfigurationProperties(AppProperties.class)
public class CampusEventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusEventsApplication.class, args);
    }
}
