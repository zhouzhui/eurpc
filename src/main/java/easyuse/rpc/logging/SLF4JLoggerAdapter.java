/*******************************************************************************
 * Copyright (c) 2012, dhf.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies, 
 * either expressed or implied, of the FreeBSD Project.
 ******************************************************************************/
package easyuse.rpc.logging;

import easyuse.rpc.Logger;

/**
 * @author dhf
 */
public class SLF4JLoggerAdapter implements Logger {
    private org.slf4j.Logger logger;

    public SLF4JLoggerAdapter(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String msg, Object[] args) {
        logger.debug(msg, args);
    }

    @Override
    public void debug(String msg, Throwable exception) {
        logger.debug(msg, exception);
    }

    @Override
    public void debug(String msg, Object[] args, Throwable exception) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg, merge(args, exception));
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String msg, Object[] args) {
        logger.info(msg, args);
    }

    @Override
    public void info(String msg, Throwable exception) {
        logger.info(msg, exception);
    }

    @Override
    public void info(String msg, Object[] args, Throwable exception) {
        if (logger.isInfoEnabled()) {
            logger.info(msg, merge(args, exception));
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(String msg, Object[] args) {
        logger.warn(msg, args);
    }

    @Override
    public void warn(String msg, Throwable exception) {
        logger.warn(msg, exception);
    }

    @Override
    public void warn(String msg, Object[] args, Throwable exception) {
        if (logger.isWarnEnabled()) {
            logger.warn(msg, merge(args, exception));
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void error(String msg, Object[] args) {
        logger.error(msg, args);
    }

    @Override
    public void error(String msg, Throwable exception) {
        logger.error(msg, exception);
    }

    @Override
    public void error(String msg, Object[] args, Throwable exception) {
        if (logger.isErrorEnabled()) {
            logger.error(msg, merge(args, exception));
        }
    }

    private Object[] merge(Object[] args, Throwable exception) {
        if (null == exception) {
            return args;
        }
        if (null == args || args.length == 0) {
            return new Object[] {
                exception
            };
        }
        Object[] result = new Object[args.length + 1];
        System.arraycopy(args, 0, result, 0, args.length);
        result[result.length - 1] = exception;
        return result;
    }
}
