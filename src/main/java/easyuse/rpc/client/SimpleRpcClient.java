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
package easyuse.rpc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;
import easyuse.rpc.Logger;
import easyuse.rpc.RpcClient;
import easyuse.rpc.RpcConnection;
import easyuse.rpc.RpcConnectionFactory;
import easyuse.rpc.util.LoggerHolder;

/**
 * @author dhf
 */
public class SimpleRpcClient implements RpcClient {
    private static final Logger logger = LoggerHolder
            .getLogger(SimpleRpcClient.class);

    private RpcConnectionFactory connectionFactory;

    private RpcConnection connection;

    private RpcInvoker invoker = new RpcInvoker();

    private AtomicLong requestID = new AtomicLong(0L);

    /**
     * @param connection
     */
    public SimpleRpcClient(RpcConnection connection) {
        if (null == connection) {
            throw new NullPointerException("connection");
        }
        this.connection = connection;
    }

    /**
     * @param factory
     */
    public SimpleRpcClient(RpcConnectionFactory factory) {
        if (null == factory) {
            throw new NullPointerException("factory");
        }
        this.connectionFactory = factory;
    }

    /**
     * get an implementation for the interface
     * 
     * @param interfaceClass
     * @return
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    public <T> T proxy(Class<T> interfaceClass) throws Throwable {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException(interfaceClass.getName()
                    + " is not an interface");
        }
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[] {
                    interfaceClass
                }, invoker);
    }

    public void destroy() throws Throwable {
        if (null != connection) {
            connection.close();
        }
    }

    protected String generateRequestID() {
        long id = requestID.getAndIncrement();
        return id + "";
    }

    private RpcConnection getConnection() throws Throwable {
        if (null != connection) {
            if (!connection.isConnected()) {
                connection.connect();
            }
            return connection;
        } else {
            return connectionFactory.getConnection();
        }
    }

    private void recycle(RpcConnection connection) {
        if (null != connection && null != connectionFactory) {
            try {
                connectionFactory.recycle(connection);
            } catch (Throwable t) {
                logger.warn("recycle rpc connection fail!", t);
            }
        }
    }

    /**
     * rpc proxy invoker
     * 
     * @author dhf
     */
    private class RpcInvoker implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            String className = method.getDeclaringClass().getName();
            List<String> parameterTypes = new LinkedList<String>();
            for (Class<?> parameterType: method.getParameterTypes()) {
                parameterTypes.add(parameterType.getName());
            }

            String requestID = generateRequestID();
            InvokeRequest request = new InvokeRequest(requestID, className,
                    method.getName(), parameterTypes.toArray(new String[0]),
                    args);
            RpcConnection connection = null;
            InvokeResponse response = null;
            try {
                connection = getConnection();
                response = connection.sendRequest(request);
            } catch (Throwable t) {
                logger.warn("send rpc request fail! request: <{}>",
                        new Object[] {
                            request
                        }, t);
                throw new RuntimeException(t);
            } finally {
                recycle(connection);
            }

            if (response.getException() != null) {
                throw response.getException();
            } else {
                return response.getResult();
            }
        }
    }
}
