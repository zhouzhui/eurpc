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
package easyuse.rpc.util;

import easyuse.rpc.Logger;
import easyuse.rpc.LoggerFactory;
import easyuse.rpc.logging.JDKLoggerFactory;

/**
 * @author dhf
 */
public class LoggerHolder {
    private static LoggerFactory FACTORY = autodetect();

    private static LoggerFactory autodetect() {
        String packageName = "easyuse.rpc.logging";
        String[] simplifiedClassNames = {
            "SLF4JLoggerFactory", "JCLLoggerFactory", "Log4j12LoggerFactory"
        };

        LoggerFactory factory = null;
        for (String simplifiedClassName: simplifiedClassNames) {
            String className = packageName + "." + simplifiedClassName;
            try {
                factory = (LoggerFactory) Class.forName(className)
                        .newInstance();
                Logger logger = factory.getLogger(className);
                logger.info("autodetect logger type: {}", new Object[] {
                    logger.getClass()
                });
                return factory;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        factory = new JDKLoggerFactory();
        Logger logger = factory.getLogger(JDKLoggerFactory.class);
        logger.info("fallback to logger type: {}", new Object[] {
            logger.getClass()
        });
        return factory;
    }

    public static Logger getLogger(Class<?> clazz) {
        return getFactory().getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return getFactory().getLogger(name);
    }

    public synchronized static void setFactory(LoggerFactory factory) {
        if (null == factory) {
            throw new NullPointerException("factory");
        }
        FACTORY = factory;
    }

    public synchronized static LoggerFactory getFactory() {
        return FACTORY;
    }
}
