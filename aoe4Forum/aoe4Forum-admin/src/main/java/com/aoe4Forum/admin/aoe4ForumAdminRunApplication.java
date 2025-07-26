package com.aoe4Forum.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(scanBasePackages = {"com.aoe4Forum"},exclude = {DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class})
public class aoe4ForumAdminRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(aoe4ForumAdminRunApplication.class, args);
    }
}
