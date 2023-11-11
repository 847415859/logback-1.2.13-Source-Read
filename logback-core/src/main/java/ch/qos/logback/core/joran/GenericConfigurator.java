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
package ch.qos.logback.core.joran;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.*;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.status.StatusUtil;

import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static ch.qos.logback.core.CoreConstants.SAFE_JORAN_CONFIGURATION;

public abstract class GenericConfigurator extends ContextAwareBase {

    private BeanDescriptionCache beanDescriptionCache;

    protected Interpreter interpreter;

    /**
     * 处理配置
     * @param url
     * @throws JoranException
     */
    public final void doConfigure(URL url) throws JoranException {
        InputStream in = null;
        try {
            // 给上下文设置加载的记录配置文件的地址
            informContextOfURLUsedForConfiguration(getContext(), url);
            // 获取配置文件流
            URLConnection urlConnection = url.openConnection();
            // per http://jira.qos.ch/browse/LBCORE-105
            // per http://jira.qos.ch/browse/LBCORE-127
            urlConnection.setUseCaches(false);

            in = urlConnection.getInputStream();
            // 核心代码: doConfigure(InputStream inputStream, String systemId)
            doConfigure(in, url.toExternalForm());
        } catch (IOException ioe) {
            String errMsg = "Could not open URL [" + url + "].";
            addError(errMsg, ioe);
            throw new JoranException(errMsg, ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    String errMsg = "Could not close input stream";
                    addError(errMsg, ioe);
                    throw new JoranException(errMsg, ioe);
                }
            }
        }
    }

    public final void doConfigure(String filename) throws JoranException {
        doConfigure(new File(filename));
    }

    public final void doConfigure(File file) throws JoranException {
        FileInputStream fis = null;
        try {
            URL url = file.toURI().toURL();
            informContextOfURLUsedForConfiguration(getContext(), url);
            fis = new FileInputStream(file);
            doConfigure(fis, url.toExternalForm());
        } catch (IOException ioe) {
            String errMsg = "Could not open [" + file.getPath() + "].";
            addError(errMsg, ioe);
            throw new JoranException(errMsg, ioe);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (java.io.IOException ioe) {
                    String errMsg = "Could not close [" + file.getName() + "].";
                    addError(errMsg, ioe);
                    throw new JoranException(errMsg, ioe);
                }
            }
        }
    }

    public static void informContextOfURLUsedForConfiguration(Context context, URL url) {
        ConfigurationWatchListUtil.setMainWatchURL(context, url);
    }

    public final void doConfigure(InputStream inputStream) throws JoranException {
        doConfigure(new InputSource(inputStream));
    }

    public final void doConfigure(InputStream inputStream, String systemId) throws JoranException {
        InputSource inputSource = new InputSource(inputStream);
        inputSource.setSystemId(systemId);
        // 核心代码: 真正执行配置解析加载 doConfigure(final InputSource inputSource)
        doConfigure(inputSource);
    }

    protected BeanDescriptionCache getBeanDescriptionCache() {
        if (beanDescriptionCache == null) {
            beanDescriptionCache = new BeanDescriptionCache(getContext());
        }
        return beanDescriptionCache;
    }

    protected abstract void addInstanceRules(RuleStore rs);

    protected abstract void addImplicitRules(Interpreter interpreter);

    protected void addDefaultNestedComponentRegistryRules(DefaultNestedComponentRegistry registry) {

    }

    protected ElementPath initialElementPath() {
        return new ElementPath();
    }

    protected void buildInterpreter() {
        // 创建规则存储实现对象
        RuleStore rs = new SimpleRuleStore(context);
        // 重要代码: 添加实例的常规规则
        // 说明: 添加规则示例: configuration/appender -> AppenderAction, configuration/logger -> LoggerAction
        // 用途: 例如AppenderActionAction是用来完成<appender>标签的解析和appender的初始化的
        addInstanceRules(rs);
        // 初始化出解析器对象, 该对象完成核心的SaxEvent解析工作
        this.interpreter = new Interpreter(context, rs, initialElementPath());
        // 初始化出解析器上下文对象, 该对象存储解析过程重要的信息
        InterpretationContext interpretationContext = interpreter.getInterpretationContext();
        interpretationContext.setContext(context);
        // 添加隐含的规则 (不展开)
        // 说明: 添加Action: NestedComplexPropertyIA 或 NestedBasicPropertyIA
        // 用途: 完成嵌套属性值的初始化和设置绑定, 例如设置appender内的encoder
        addImplicitRules(interpreter);
        // 添加默认嵌套组件注册表 (不展开)
        // 说明: 指定嵌套组件的默认实现, 例如UnsynchronizedAppenderBase的encoder属性默认实现为PatternLayoutEncode
        addDefaultNestedComponentRegistryRules(interpretationContext.getDefaultNestedComponentRegistry());
    }

    // this is the most inner form of doConfigure whereto other doConfigure
    // methods ultimately delegate

    /**
     * 真正执行代码逻辑，其他方法都是向他委派
     * @param inputSource
     * @throws JoranException
     */
    public final void doConfigure(final InputSource inputSource) throws JoranException {

        long threshold = System.currentTimeMillis();
        // if (!ConfigurationWatchListUtil.wasConfigurationWatchListReset(context)) {
        // informContextOfURLUsedForConfiguration(getContext(), null);
        // }
        // 使用SAXParser解析配置文件, 得到saxEventList
        SaxEventRecorder recorder = new SaxEventRecorder(context);
        recorder.recordEvents(inputSource);
        // 核心代码: 执行配置
        doConfigure(recorder.saxEventList);
        // no exceptions a this level
        // 没有XML解析错误发生时, 将当前配置注册为安全回退点
        StatusUtil statusUtil = new StatusUtil(context);
        if (statusUtil.noXMLParsingErrorsOccurred(threshold)) {
            addInfo("Registering current configuration as safe fallback point");
            registerSafeConfiguration(recorder.saxEventList);
        }
    }

    public void doConfigure(final List<SaxEvent> eventList) throws JoranException {
        // 重要代码: 构建解析器 (注: 假设GenericConfigurator的实现类为JoranConfigurator (默认值))
        buildInterpreter();
        // disallow simultaneous configurations of the same context
        // 核心代码: 上锁, 解析一个个SaxEvent事件(以栈的格式逐个处理配置文件中的各个标签解析出来的内容), 完成LoggerContext初始化
        synchronized (context.getConfigurationLock()) {
            interpreter.getEventPlayer().play(eventList);
        }
    }

    /**
     * Register the current event list in currently in the interpreter as a safe
     * configuration point.
     *
     * @since 0.9.30
     */
    public void registerSafeConfiguration(List<SaxEvent> eventList) {
        context.putObject(SAFE_JORAN_CONFIGURATION, eventList);
    }

    /**
     * Recall the event list previously registered as a safe point.
     */
    @SuppressWarnings("unchecked")
    public List<SaxEvent> recallSafeConfiguration() {
        return (List<SaxEvent>) context.getObject(SAFE_JORAN_CONFIGURATION);
    }
}
