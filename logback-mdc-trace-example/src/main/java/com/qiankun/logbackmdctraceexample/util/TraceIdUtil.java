package com.qiankun.logbackmdctraceexample.util;

/**
 * @Description:
 * @Date : 2023/11/13 16:20
 * @Auther : tiankun
 */
public class TraceIdUtil {

    public static String getTraceId() {
        return Thread.currentThread().getId() + "";
    }
}
