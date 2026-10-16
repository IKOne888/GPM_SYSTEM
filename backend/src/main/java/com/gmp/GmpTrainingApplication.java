package com.gmp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * GMP内部培训系统主启动类
 */
@SpringBootApplication
@MapperScan("com.gmp.mapper")
@EnableScheduling
public class GmpTrainingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmpTrainingApplication.class, args);
        System.out.println("====================================");
        System.out.println("GMP内部培训系统启动成功！");
        System.out.println("接口文档地址: http://localhost:8080/api/doc.html");
        System.out.println("====================================");
    }
}
