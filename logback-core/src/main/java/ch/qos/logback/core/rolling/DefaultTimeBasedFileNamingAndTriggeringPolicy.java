/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.rolling;

import java.io.File;
import java.util.Date;

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.helper.TimeBasedArchiveRemover;

/**
 * 
 * @author Ceki G&uuml;lc&uuml;
 * 
 * @param <E>
 */
@NoAutoStart
public class DefaultTimeBasedFileNamingAndTriggeringPolicy<E> extends TimeBasedFileNamingAndTriggeringPolicyBase<E> {

    @Override
    public void start() {
        super.start();
        if (!super.isErrorFree())
            return;
        if(tbrp.fileNamePattern.hasIntegerTokenCOnverter()) {
            addError("Filename pattern ["+tbrp.fileNamePattern+"] contains an integer token converter, i.e. %i, INCOMPATIBLE with this configuration. Remove it.");
            return;
        }
        
        archiveRemover = new TimeBasedArchiveRemover(tbrp.fileNamePattern, rc);
        archiveRemover.setContext(context);
        started = true;
    }

    public boolean isTriggeringEvent(File activeFile, final E event) {
        // 判断时间是否超过 上次算出来的下次滚动时间. 这里是根据输入的滚动文件时间格式获取滚动时间间隔, 逻辑与log4j类似, 这里不展开
        //   获取时间间隔单位逻辑(RollingCalendar.computePeriodicityType):
        //      遍历的时间单位从小到大判断(毫秒->秒...->月)
        //          取一个1970年0点时间epoch, 按照配置的滚动日期格式格式化得到ro,
        //          epoch加上遍历单位1个单位的值, 再按照配置的滚动日期格式格式化得到r1,
        //          判断两个日期是否相等, 如果不相等, 则返回当前遍历的时间单位 (类似于整数舍弃余数的方式 判断累加数是否会使整数值变化)
        long time = getCurrentTime();
        if (time >= nextCheck) {
            // 获取上次的进入isTriggeringEvent方法获取的时间戳time
            Date dateOfElapsedPeriod = dateInCurrentPeriod;
            addInfo("Elapsed period: " + dateOfElapsedPeriod);
            // 获取需要滚动文件的替换文件名. 这里涉及到了转换器Converter的初始化和使用, 本文讲述layout的地方也讲到, 故不展开
            // 比如： tbrp.fileNamePatternWithoutCompSuffix=./logs//info/info.%d{yyyy-MM-dd HH:mm}.log
            elapsedPeriodsFileName = tbrp.fileNamePatternWithoutCompSuffix.convert(dateOfElapsedPeriod);
            // 刷新dateInCurrentPeriod=time, 不展开
            setDateInCurrentPeriod(time);
            // 刷新下次滚动时间 nextCheck（时间戳）, 不展开
            computeNextCheck();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "c.q.l.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy";
    }
}
