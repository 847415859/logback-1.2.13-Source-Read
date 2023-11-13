package com.qiankun.logbackmdctraceexample.interceptor;

import com.qiankun.logbackmdctraceexample.constant.Constants;
import com.qiankun.logbackmdctraceexample.util.TraceIdUtil;
import org.aopalliance.intercept.Interceptor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

/**
 * @Description:
 * @Date : 2023/11/13 16:15
 * @Auther : tiankun
 */
@Component
public class MDCInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = request.getHeader(Constants.TRACE_ID);
        if (traceId != null) {
            MDC.put(Constants.TRACE_ID,traceId);
            return true;
        }
        String tractId = MDC.get(Constants.TRACE_ID);
        if (tractId == null) {
            tractId = TraceIdUtil.getTraceId();
        }
        MDC.put(Constants.TRACE_ID, tractId);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove(Constants.TRACE_ID);
    }
}
