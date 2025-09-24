package com.hoquangnam45.cdc.image.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.hoquangnam45.cdc.image.app")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
