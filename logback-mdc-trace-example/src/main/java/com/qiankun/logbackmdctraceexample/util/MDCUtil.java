package com.qiankun.logbackmdctraceexample.util;

import com.qiankun.logbackmdctraceexample.constant.Constants;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

public class MDCUtil {
    public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
                MDC.clear();
            }else {
                MDC.setContextMap(context);
            }
            //如果不是子线程的话先生成traceId
            setTraceIdIfAbsent();
            try {
                runnable.run();
            }finally {
                MDC.clear();
            }
        };
    }


    public static <T> Callable<T> wrap(final Callable<T> callable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            try {
                return ((T) callable.call());
            }finally {
                MDC.clear();
            }
        };

    }

    public static void setTraceIdIfAbsent() {
        if (MDC.get(Constants.TRACE_ID) == null) {
            MDC.put(Constants.TRACE_ID, TraceIdUtil.getTraceId());
        }
    }
}
