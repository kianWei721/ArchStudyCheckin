package com.archstudy.checkin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.archstudy.checkin.**.mapper")
public class ArchStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchStudyApplication.class, args);
    }
}
