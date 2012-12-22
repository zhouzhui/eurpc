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
package easyuse.rpc.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
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
import easyuse.rpc.RpcConnection;
import easyuse.rpc.util.SocketConfig;

/**
 * @author dhf
 */
public class NettyRpcConnection extends SimpleChannelHandler implements
        RpcConnection {

    private InetSocketAddress inetAddr;

    private ClientSerializer serializer;

    private volatile Channel channel;

    private volatile InvokeResponse response;

    private volatile Throwable exception;

    private volatile Timer timer;

    private SocketConfig socketOptions;

    private boolean connected;

    /**
     * tcpNoDelay: true, keepAlive: true, connectTimeout: infinite, readTimeout:
     * infinite
     * 
     * @param host
     * @param port
     * @param serializer
     */
    public NettyRpcConnection(String host, int port, ClientSerializer serializer) {
        this(host, port, serializer, null);
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
    public NettyRpcConnection(String host, int port,
            ClientSerializer serializer, long connectTimeout, long readTimeout) {
        this(host, port, serializer, new SocketConfig().setKeepAlive(true)
                .setTcpNoDelay(true).setConnectTimeout((int) connectTimeout)
                .setReadTimeout((int) readTimeout));
    }

    public NettyRpcConnection(String host, int port,
            ClientSerializer serializer, SocketConfig socketOptions) {
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

    public void setSocketOptions(SocketConfig socketOptions) {
        this.socketOptions = socketOptions;
    }

    public void connect() throws Throwable {
        if (connected) {
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
                pipeline.addLast("handler", NettyRpcConnection.this);
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
        connected = true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isClosed() {
        return (null == channel) || !channel.isConnected()
                || !channel.isReadable() || !channel.isWritable();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        exception = e.getCause();
        synchronized (channel) {
            channel.notifyAll();
        }
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
    public InvokeResponse sendRequest(InvokeRequest request) throws Throwable {
        if (!isConnected()) {
            throw new IllegalStateException("not connected");
        }
        ChannelFuture writeFuture = channel.write(request);
        if (!writeFuture.awaitUninterruptibly().isSuccess()) {
            close();
            throw writeFuture.getCause();
        }
        waitForResponse();

        Throwable ex = exception;
        InvokeResponse resp = this.response;
        this.response = null;
        this.exception = null;

        if (null != ex) {
            close();
            throw ex;
        }
        return resp;
    }

    @Override
    public void close() throws Exception {
        connected = false;
        if (null != timer) {
            timer.stop();
            timer = null;
        }
        if (null != channel) {
            channel.close().awaitUninterruptibly();
            channel.getFactory().releaseExternalResources();

            this.exception = new IOException("connection closed");
            synchronized (channel) {
                channel.notifyAll();
            }
            channel = null;
        }
    }
}
