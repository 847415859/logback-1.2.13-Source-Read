package com.qiankun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description:
 * @Date : 2023/11/11 10:18
 * @Auther : tiankun
 */
public class TestBoot {
    public static void main(String[] args) throws InterruptedException {
        Logger logger = LoggerFactory.getLogger(TestBoot.class);
        int i = 0;
        while (true){
            Thread.sleep(2000);
            // 入口
            logger.info("打印INFO日志：{}","qiankun"+(i++));
        }
    }
}
