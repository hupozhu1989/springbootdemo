package com.wei.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 功能模块描述
 *
 * @Author Forward
 * @Date 2021/6/30 10:09
 */
@SpringBootApplication
public class StudyController {
    //java命令启动    java -jar springbootdemo-1.0-SNAPSHOT.jar --server.port=9001
    public static void main(String[] args) {
        SpringApplication.run(StudyController.class, args);
    }
}
