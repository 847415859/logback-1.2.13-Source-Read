package com.qiankun.logbackmdctraceexample.config;

import com.qiankun.logbackmdctraceexample.interceptor.MDCInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description:
 * @Date : 2023/11/13 16:29
 * @Auther : tiankun
 */
@Configurable
public class MvcInterceptorConfig implements WebMvcConfigurer {

    @Autowired
    MDCInterceptor mdcInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mdcInterceptor)
                .addPathPatterns("/**");
    }
}
