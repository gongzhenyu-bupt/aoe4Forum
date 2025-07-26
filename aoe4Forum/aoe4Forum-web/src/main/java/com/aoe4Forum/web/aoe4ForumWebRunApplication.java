package com.aoe4Forum.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.aoe4Forum"})
@MapperScan("com.aoe4Forum.mapper")
@EnableRabbit
public class aoe4ForumWebRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(aoe4ForumWebRunApplication.class, args);
    }
}
