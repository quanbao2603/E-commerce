package com.womtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.womtech.repository")
@EntityScan("com.womtech.entity")
public class WomtechApplication {
    public static void main(String[] args) {
        SpringApplication.run(WomtechApplication.class, args);
    }
}
