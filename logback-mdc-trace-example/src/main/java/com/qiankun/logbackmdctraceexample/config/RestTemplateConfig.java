package com.qiankun.logbackmdctraceexample.config;

import com.qiankun.logbackmdctraceexample.constant.Constants;
import com.qiankun.logbackmdctraceexample.util.MDCUtil;
import com.qiankun.logbackmdctraceexample.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

/**
 * @Description: RestTemplate 配置类，添加拦截器
 * @Date : 2023/11/13 16:32
 * @Auther : tiankun
 */
@Configurable
public class RestTemplateConfig {

    private static Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    public RestTemplate restTemplate(){
        RestTemplate restTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        interceptors.add(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                logger.info("RestTemplate 拦截器...");
                String traceId = MDC.get(Constants.TRACE_ID);
                if(traceId != null){
                    request.getHeaders().add(Constants.TRACE_ID,traceId);
                }
                MDCUtil.setTraceIdIfAbsent();
                return execution.execute(request,body);
            }
        });
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
