package com.campus.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类。
 */
@EnableScheduling
@SpringBootApplication
public class ActivityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication.class, args);
    }
}
