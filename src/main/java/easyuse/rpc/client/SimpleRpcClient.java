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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import easyuse.rpc.ClientSerializer;
import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;
import easyuse.rpc.RpcClient;
import easyuse.rpc.util.IOUtils;
import easyuse.rpc.util.SocketConfig;

/**
 * jdk dynamic proxy
 * 
 * @author dhf
 */
public class SimpleRpcClient implements RpcClient, InvocationHandler {
    private InetSocketAddress inetAddr;

    private ClientSerializer serializer;

    private SocketConfig socketOptions;

    private Socket socket;

    private InputStream in;

    private OutputStream out;

    private boolean inited;

    /**
     * tcpNoDelay: true, keepAlive: true, connectTimeout: infinite, readTimeout:
     * infinite
     * 
     * @param host
     * @param port
     * @param serializer
     */
    public SimpleRpcClient(String host, int port, ClientSerializer serializer) {
        this(host, port, serializer, 0L, 0L);
    }

    /**
     * tcpNoDelay: true, keepAlive: true
     * 
     * @param host
     * @param port
     * @param serializer
     * @param connectTimeout
     * @param readTimeout
     */
    public SimpleRpcClient(String host, int port, ClientSerializer serializer,
            long connectTimeout, long readTimeout) {
        this(host, port, serializer, new SocketConfig().setKeepAlive(true)
                .setTcpNoDelay(true).setConnectTimeout((int) connectTimeout)
                .setReadTimeout((int) readTimeout));
    }

    /**
     * @param host
     * @param port
     * @param serializer
     * @param socketOptions
     */
    public SimpleRpcClient(String host, int port, ClientSerializer serializer,
            SocketConfig socketOptions) {
        if (null == serializer) {
            throw new NullPointerException("serializer");
        }
        if (null == socketOptions) {
            socketOptions = new SocketConfig();
        }

        this.inetAddr = new InetSocketAddress(host, port);
        this.serializer = serializer;
        this.socketOptions = socketOptions;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T proxy(Class<T> interfaceClass) throws Throwable {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException(interfaceClass.getName()
                    + " is not an interface");
        }
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[] { interfaceClass }, this);
    }

    public void init() throws Throwable {
        if (inited) {
            return;
        }
        socket = new Socket();
        IOUtils.setSocketOptions(socket, socketOptions).connect(inetAddr,
                socketOptions.getConnectTimeout());
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
        inited = true;
    }

    @Override
    public void close() throws Throwable {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(socket);
        
        in = null;
        out = null;
        socket = null;
        inited = false;
    }
    
    @Override
    public boolean isInited() {
        return inited;
    }

    @Override
    public boolean isClosed() {
        return (null == socket) || !socket.isConnected() || socket.isClosed()
                || socket.isInputShutdown() || socket.isOutputShutdown();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        init();
        // get interface
        String className = method.getDeclaringClass().getName();
        List<String> parameterTypes = new LinkedList<String>();
        for (Class<?> parameterType: method.getParameterTypes()) {
            parameterTypes.add(parameterType.getName());
        }

        InvokeRequest request = new InvokeRequest(className, method.getName(),
                parameterTypes.toArray(new String[0]), args);
        serializer.encodeRequest(out, request);
        out.flush();
        InvokeResponse response = serializer.decodeResponse(in);
        if (response.getException() != null) {
            throw response.getException();
        } else {
            return response.getResult();
        }
    }

}
