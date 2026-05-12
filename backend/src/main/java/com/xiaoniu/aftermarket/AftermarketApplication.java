package com.xiaoniu.aftermarket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.xiaoniu.aftermarket.*.mapper")
@SpringBootApplication
public class AftermarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(AftermarketApplication.class, args);
    }
}
