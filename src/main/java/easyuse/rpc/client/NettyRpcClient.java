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
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import easyuse.rpc.ClientSerializer;
import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;
import easyuse.rpc.RpcClient;
import easyuse.rpc.util.SocketConfig;

/**
 * @author dhf
 */
public class NettyRpcClient extends SimpleChannelHandler implements RpcClient,
        InvocationHandler {

    private InetSocketAddress inetAddr;

    private ClientSerializer serializer;

    private volatile Channel channel;

    private volatile InvokeResponse response;

    private Timer timer;

    private SocketConfig socketOptions;

    /**
     * tcpNoDelay: true, keepAlive: true, connectTimeout: infinite, readTimeout:
     * infinite
     * 
     * @param host
     * @param port
     * @param serializer
     */
    public NettyRpcClient(String host, int port, ClientSerializer serializer) {
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
    public NettyRpcClient(String host, int port, ClientSerializer serializer,
            long connectTimeout, long readTimeout) {
        this(host, port, serializer, new SocketConfig().setKeepAlive(true)
                .setTcpNoDelay(true).setConnectTimeout((int) connectTimeout)
                .setReadTimeout((int) readTimeout));
    }

    public NettyRpcClient(String host, int port, ClientSerializer serializer,
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
        this.timer = new HashedWheelTimer();
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
        if (null != channel) {
            return;
        }
        ChannelFactory factory = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() {
                ChannelPipeline pipeline = Channels.pipeline();

                int readTimeout = socketOptions.getReadTimeout();
                if (readTimeout > 0) {
                    pipeline.addLast("timeout", new ReadTimeoutHandler(timer,
                            readTimeout, TimeUnit.MILLISECONDS));
                }

                pipeline.addLast("decoder", new InvokeResponseDecoder(
                        serializer));
                pipeline.addLast("encoder",
                        new InvokeRequestEncoder(serializer));
                pipeline.addLast("handler", NettyRpcClient.this);
                return pipeline;
            }
        });

        if (socketOptions.getOptions().size() > 0) {
            bootstrap.setOptions(socketOptions.getOptions());
        }
        ChannelFuture channelFuture = bootstrap.connect(inetAddr);
        if (!channelFuture.awaitUninterruptibly().isSuccess()) {
            bootstrap.releaseExternalResources();
            throw channelFuture.getCause();
        }
        channel = channelFuture.getChannel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        InvokeResponse resp = new InvokeResponse();
        resp.setException(e.getCause());
        response = resp;
        close();
    }

    public boolean isClosed() {
        return !channel.isConnected() || !channel.isReadable()
                || !channel.isWritable();
    }

    public void waitForResponse() {
        synchronized (channel) {
            try {
                channel.wait();
            } catch (InterruptedException e) {}
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        response = (InvokeResponse) e.getMessage();
        synchronized (channel) {
            channel.notifyAll();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        init();
        String className = method.getDeclaringClass().getName();
        List<String> parameterTypes = new LinkedList<String>();
        for (Class<?> parameterType: method.getParameterTypes()) {
            parameterTypes.add(parameterType.getName());
        }

        InvokeRequest request = new InvokeRequest(className, method.getName(),
                parameterTypes.toArray(new String[0]), args);
        ChannelFuture writeFuture = channel.write(request);
        if (!writeFuture.awaitUninterruptibly().isSuccess()) {
            close();
            throw writeFuture.getCause();
        }
        waitForResponse();
        InvokeResponse resp = this.response;
        this.response = null;
        if (resp.getException() != null) {
            throw resp.getException();
        } else {
            return resp.getResult();
        }
    }

    @Override
    public void close() throws Exception {
        if (!isClosed()) {
            channel.close().awaitUninterruptibly();
            channel.getFactory().releaseExternalResources();
            timer.stop();
            synchronized (channel) {
                channel.notifyAll();
            }
        }
    }
}
