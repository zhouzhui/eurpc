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

import org.apache.commons.logging.Log;

import easyuse.rpc.Logger;
import easyuse.rpc.util.MessageFormatter;

/**
 * apache commons logging logger
 * 
 * @author dhf
 */
public class JCLLoggerAdapter implements Logger {
    private Log logger;

    public JCLLoggerAdapter(Log logger) {
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
        if (isDebugEnabled()) {
            msg = MessageFormatter.format(msg, args);
            logger.debug(msg);
        }
    }

    @Override
    public void debug(String msg, Throwable exception) {
        logger.debug(msg, exception);
    }

    @Override
    public void debug(String msg, Object[] args, Throwable exception) {
        if (isDebugEnabled()) {
            msg = MessageFormatter.format(msg, args);
            logger.debug(msg, exception);
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
        if (isInfoEnabled()) {
            msg = MessageFormatter.format(msg, args);
            logger.info(msg);
        }
    }

    @Override
    public void info(String msg, Throwable exception) {
        logger.info(msg, exception);
    }

    @Override
    public void info(String msg, Object[] args, Throwable exception) {
        if (isInfoEnabled()) {
            msg = MessageFormatter.format(msg, args);
            logger.info(msg, exception);
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
        if (isWarnEnabled()) {
            msg = MessageFormatter.format(msg, args);
            logger.warn(msg);
        }
    }

    @Override
    public void warn(String msg, Throwable exception) {
        logger.warn(msg, exception);
    }

    @Override
    public void warn(String msg, Object[] args, Throwable exception) {
        if (isWarnEnabled()) {
            msg = MessageFormatter.format(msg, args);
            logger.warn(msg, exception);
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
        if (isErrorEnabled()) {
            msg = MessageFormatter.format(msg, args);
            logger.error(msg);
        }
    }

    @Override
    public void error(String msg, Throwable exception) {
        logger.error(msg, exception);
    }

    @Override
    public void error(String msg, Object[] args, Throwable exception) {
        if (isErrorEnabled()) {
            msg = MessageFormatter.format(msg, args);
            logger.error(msg, exception);
        }
    }
}
