package com.earseo.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BackendCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendCoreApplication.class, args);
    }

}
