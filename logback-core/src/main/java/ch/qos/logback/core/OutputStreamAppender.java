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
package ch.qos.logback.core;

import static ch.qos.logback.core.CoreConstants.CODES_URL;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantLock;

import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;

/**
 * OutputStreamAppender appends events to a {@link OutputStream}. This class
 * provides basic services that other appenders build upon.
 * 
 * For more information about this appender, please refer to the online manual
 * at http://logback.qos.ch/manual/appenders.html#OutputStreamAppender
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class OutputStreamAppender<E> extends UnsynchronizedAppenderBase<E> {

    /**
     * It is the encoder which is ultimately responsible for writing the event to
     * an {@link OutputStream}.
     */
    protected Encoder<E> encoder;

    /**
     * All synchronization in this class is done via the lock object.
     */
    protected final ReentrantLock lock = new ReentrantLock(false);

    /**
     * This is the {@link OutputStream outputStream} where output will be written.
     */
    private OutputStream outputStream;

    boolean immediateFlush = true;

    /**
    * The underlying output stream used by this appender.
    * 
    * @return
    */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Checks that requires parameters are set and if everything is in order,
     * activates this appender.
     */
    public void start() {
        int errors = 0;
        if (this.encoder == null) {
            addStatus(new ErrorStatus("No encoder set for the appender named \"" + name + "\".", this));
            errors++;
        }

        if (this.outputStream == null) {
            addStatus(new ErrorStatus("No output stream set for the appender named \"" + name + "\".", this));
            errors++;
        }
        // only error free appenders should be activated
        if (errors == 0) {
            super.start();
        }
    }

    public void setLayout(Layout<E> layout) {
        addWarn("This appender no longer admits a layout as a sub-component, set an encoder instead.");
        addWarn("To ensure compatibility, wrapping your layout in LayoutWrappingEncoder.");
        addWarn("See also " + CODES_URL + "#layoutInsteadOfEncoder for details");
        LayoutWrappingEncoder<E> lwe = new LayoutWrappingEncoder<E>();
        lwe.setLayout(layout);
        lwe.setContext(context);
        this.encoder = lwe;
    }

    @Override
    protected void append(E eventObject) {
        // 确保当前appender已完成初始化 (因为其他线程使用该appender出错时start属性就变成了false)
        if (!isStarted()) {
            return;
        }
        // 核心代码: 执行日志输出
        subAppend(eventObject);
    }

    /**
     * Stop this appender instance. The underlying stream or writer is also
     * closed.
     * 
     * <p>
     * Stopped appenders cannot be reused.
     */
    public void stop() {
        lock.lock();
        try {
            closeOutputStream();
            super.stop();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Close the underlying {@link OutputStream}.
     */
    protected void closeOutputStream() {
        if (this.outputStream != null) {
            try {
                // before closing we have to output out layout's footer
                encoderClose();
                this.outputStream.close();
                this.outputStream = null;
            } catch (IOException e) {
                addStatus(new ErrorStatus("Could not close output stream for OutputStreamAppender.", this, e));
            }
        }
    }

    void encoderClose() {
        if (encoder != null && this.outputStream != null) {
            try {
                byte[] footer = encoder.footerBytes();
                writeBytes(footer);
            } catch (IOException ioe) {
                this.started = false;
                addStatus(new ErrorStatus("Failed to write footer for appender named [" + name + "].", this, ioe));
            }
        }
    }

    /**
     * <p>
     * Sets the @link OutputStream} where the log output will go. The specified
     * <code>OutputStream</code> must be opened by the user and be writable. The
     * <code>OutputStream</code> will be closed when the appender instance is
     * closed.
     * 
     * @param outputStream
     *          An already opened OutputStream.
     */
    public void setOutputStream(OutputStream outputStream) {
        lock.lock();
        try {
            // close any previously opened output stream
            closeOutputStream();
            this.outputStream = outputStream;
            if (encoder == null) {
                addWarn("Encoder has not been set. Cannot invoke its init method.");
                return;
            }

            encoderInit();
        } finally {
            lock.unlock();
        }
    }

    void encoderInit() {
        if (encoder != null && this.outputStream != null) {
            try {
                byte[] header = encoder.headerBytes();
                writeBytes(header);
            } catch (IOException ioe) {
                this.started = false;
                addStatus(new ErrorStatus("Failed to initialize encoder for appender named [" + name + "].", this, ioe));
            }
        }
    }
    protected void writeOut(E event) throws IOException {
        byte[] byteArray = this.encoder.encode(event);
        writeBytes(byteArray);
    }

    private void writeBytes(byte[] byteArray) throws IOException {
        if(byteArray == null || byteArray.length == 0)
            return;
        // 上锁, 避免多线程写入写出时出现错误
        lock.lock();
        try {
            // 写入输出流中
            this.outputStream.write(byteArray);
            //  若immediateFlush=true, 立即写出. 该属性默认值为true
            if (immediateFlush) {
                this.outputStream.flush();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Actual writing occurs here.
     * <p>
     * Most subclasses of <code>WriterAppender</code> will need to override this
     * method.
     * 
     * @since 0.9.0
     */
    protected void subAppend(E event) {
        // 确保当前appender已完成初始化(因为其他线程使用该appender出错时start属性就变成了false)
        if (!isStarted()) {
            return;
        }
        try {
            // this step avoids LBCLASSIC-139
            // 设置LoggerEvent的内容, 线程名, 和mdc属性值. 不展开
            if (event instanceof DeferredProcessingAware) {
                ((DeferredProcessingAware) event).prepareForDeferredProcessing();
            }
            // 使用编码器的解析出最终的日志内容. 这里由于每个转换器都保证了线程安全, 故该方法不用上锁
            // 注: 后续假设Encoder的实现类为PatternLayoutEncoder
            byte[] byteArray = this.encoder.encode(event);
            //  使用流输出日志信息. 为避免多线程出现写入写出, 该方法需要使用同步锁
            writeBytes(byteArray);

        } catch (IOException ioe) {
            // 异常则该appender就关闭掉了
            this.started = false;
            addStatus(new ErrorStatus("IO failure in appender", this, ioe));
        }
    }

    public Encoder<E> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    public boolean isImmediateFlush() {
        return immediateFlush;
    }

    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }

}
