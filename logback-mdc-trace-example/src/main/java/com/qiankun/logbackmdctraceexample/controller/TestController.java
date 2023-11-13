package com.qiankun.logbackmdctraceexample.controller;

import com.qiankun.logbackmdctraceexample.config.MvcInterceptorConfig;
import com.qiankun.logbackmdctraceexample.config.RestTemplateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @Description:
 * @Date : 2023/11/13 16:25
 * @Auther : tiankun
 */
@RestController
@RequestMapping("test")
@Import(value = {MvcInterceptorConfig.class, RestTemplateConfig.class})
public class TestController {

    private static Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("test")
    public Object test(){
        logger.info("调用其他服务");
        String forObject = restTemplate.getForObject("http://127.0.0.1:8080/invoke/test", String.class);
        logger.info("调用其他服务成功,返回值：{}",restTemplate);
        return forObject;
    }
}
