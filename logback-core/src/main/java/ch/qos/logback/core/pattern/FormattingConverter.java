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
package ch.qos.logback.core.pattern;

abstract public class FormattingConverter<E> extends Converter<E> {

    static final int INITIAL_BUF_SIZE = 256;
    static final int MAX_CAPACITY = 1024;

    FormatInfo formattingInfo;

    final public FormatInfo getFormattingInfo() {
        return formattingInfo;
    }

    final public void setFormattingInfo(FormatInfo formattingInfo) {
        if (this.formattingInfo != null) {
            throw new IllegalStateException("FormattingInfo has been already set");
        }
        this.formattingInfo = formattingInfo;
    }

    @Override
    final public void write(StringBuilder buf, E event) {
        // 执行子类convert方法获取内容
        String s = convert(event);
        // 根据配置的格式格式化内容
        // FormatInfo属性
        //  min: 小数点前的正整数, 表示最小长度. min前面的"-"表示leftPad取反, 即左对齐
        //  max: 小数点后的正整数, 表示最大长度. max前面的"-"表示leftTruncate取反, 即截掉右边, 保留左边(长度超出时)
        //  leftPad: 左边加空格, 即右对齐
        //  leftTruncate: 截掉左边, 保留右边(长度超出时)
        //  示例说明:
        //      %-2.5method表示输出的方法名 最小长度为2, 最大长度为5, 左对齐(小于最小长度右边补空格), 超出长度截掉左边, 保留右边
        //          "s"方法                    -> "s "
        //          "soLongMethodNameIsMy"方法 -> "eIsMy"
        //      %2.-5method表示输出的方法名 最小长度为2, 最大长度为3, 右对齐(小于最小长度左边补空格), 超出长度截掉右边, 保留左边
        //          "s"方法                    -> " s"
        //          "soLongMethodNameIsMy"方法 -> "soLon"
        if (formattingInfo == null) {
            // 没有配置格式化信息, 直接返回
            buf.append(s);
            return;
        }
        // 小数点左边的数字 (示例%-2.5level中的 2)
        int min = formattingInfo.getMin();
        // 小数点右边的数字 (示例%-2.5level中的 5)
        int max = formattingInfo.getMax();

        if (s == null) {
            if (0 < min)
                // 补充min个空格. 不展开
                SpacePadder.spacePad(buf, min);
            return;
        }

        int len = s.length();

        if (len > max) {
            // 截掉左边, 保留右边
            if (formattingInfo.isLeftTruncate()) {
                buf.append(s.substring(len - max));
            } else {
                // 截掉右边, 保留左边
                buf.append(s.substring(0, max));
            }
        // min控制最小长度. 小于最小长度则添加空格
        } else if (len < min) {
            if (formattingInfo.isLeftPad()) {
                // 右对齐, 则左边加空格
                SpacePadder.leftPad(buf, s, min);
            } else {
                // 左对齐, 则右边加空格
                SpacePadder.rightPad(buf, s, min);
            }
        } else {
            buf.append(s);
        }
    }
}
