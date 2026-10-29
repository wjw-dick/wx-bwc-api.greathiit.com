package com.greathiit.bwc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园保卫安防管理系统（SaaS）后端入口。
 */
@EnableScheduling
@MapperScan("com.greathiit.bwc.module.**.mapper")
@SpringBootApplication
public class BwcApplication {

    public static void main(String[] args) {
        SpringApplication.run(BwcApplication.class, args);
    }
}
