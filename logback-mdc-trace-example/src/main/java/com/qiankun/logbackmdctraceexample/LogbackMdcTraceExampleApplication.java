package com.qiankun.logbackmdctraceexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.qiankun"})
public class LogbackMdcTraceExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogbackMdcTraceExampleApplication.class, args);
    }

}
