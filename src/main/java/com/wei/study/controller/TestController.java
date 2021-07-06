package com.wei.study.controller;

import com.wei.study.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能模块描述
 *
 * @Author Forward
 * @Date 2021/6/30 10:08
 */
@RestController
@RequestMapping("/hello")
public class TestController {
    private  final Logger logger = LoggerFactory.getLogger(TestController.class);
    @Autowired
    private ServerConfig serverConfig;

    @GetMapping("/url")
    public String getUrl(){
        logger.info("请求来了。。。");
        return "返回地址>>> "+serverConfig.getUrl();
    }


}
