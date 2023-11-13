package com.qiankun.logbackmdctraceexample.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @Description:
 * @Date : 2023/11/13 16:37
 * @Auther : tiankun
 */
@RestController
@RequestMapping("invoke")
public class InvokerController {

    private static Logger logger = LoggerFactory.getLogger(TestController.class);


    @GetMapping("test")
    public String test(){
        logger.info("被调用服务....");
        return "invoke.test";
    }
}
