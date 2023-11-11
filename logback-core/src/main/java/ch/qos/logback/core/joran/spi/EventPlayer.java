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
package ch.qos.logback.core.joran.spi;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.joran.event.BodyEvent;
import ch.qos.logback.core.joran.event.EndEvent;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.StartEvent;

public class EventPlayer {
    // 解析器
    final Interpreter interpreter;
    // xml解析出来的事件列表
    List<SaxEvent> eventList;
    // 当前解析事件的索引
    int currentIndex;

    public EventPlayer(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * Return a copy of the current event list in the player.
     * @return
     * @since 0.9.20
     */
    public List<SaxEvent> getCopyOfPlayerEventList() {
        return new ArrayList<SaxEvent>(eventList);
    }

    public void play(List<SaxEvent> aSaxEventList) {
        eventList = aSaxEventList;
        SaxEvent se;
        // 遍历SaxEventList. 可以理解为逐个解析xml中的每一个标签(以栈的形式处理)
        for (currentIndex = 0; currentIndex < eventList.size(); currentIndex++) {
            se = eventList.get(currentIndex);
            // 处理开始事件, 示例: 解析<appender name="console" class="ch.qos.logback.core.ConsoleAppender">
            if (se instanceof StartEvent) {
                // 核心代码: 解析标签开始元素
                interpreter.startElement((StartEvent) se);
                // invoke fireInPlay after startElement processing
                // 将当前事件添加到所有解析器的监听器中待后续使用 (非核心代码, 不展开)
                interpreter.getInterpretationContext().fireInPlay(se);
            }
            // 处理body事件, 示例: 解析<pattern>${log.pattern}</pattern>中的${log.pattern}
            if (se instanceof BodyEvent) {
                // invoke fireInPlay before characters processing
                // 将当前事件添加到所有解析器的监听器中待后续使用 (非核心代码, 不展开)
                interpreter.getInterpretationContext().fireInPlay(se);
                // 核心代码: 解析标签体内的字符串
                interpreter.characters((BodyEvent) se);
            }
            // 处理结束时事件, 示例: 解析</appender>
            if (se instanceof EndEvent) {
                // invoke fireInPlay before endElement processing
                // 将当前事件添加到所有解析器的监听器中待后续使用 (非核心代码, 不展开)
                interpreter.getInterpretationContext().fireInPlay(se);
                //  核心代码: 解析标签结束元素
                interpreter.endElement((EndEvent) se);
            }

        }
    }

    public void addEventsDynamically(List<SaxEvent> eventList, int offset) {
        this.eventList.addAll(currentIndex + offset, eventList);
    }
}
