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
package easyuse.rpc.server;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import easyuse.rpc.RpcServer;
import easyuse.rpc.ServerSerializer;
import easyuse.rpc.util.HandlerMapper;
import easyuse.rpc.util.SocketConfig;

/**
 * @author dhf
 */
public class NettyRpcServer implements RpcServer {
    protected InetSocketAddress inetAddr;

    protected Map<String, Object> handlersMap;

    private AtomicBoolean stopped = new AtomicBoolean(false);

    private ServerSerializer serializer;

    private SocketConfig socketOptions;

    private SocketConfig childSocketOptions;

    /**
     * tcpNoDelay: true, keepAlive: true, readTimeout: infinite
     * 
     * @param port
     * @param serializer
     * @param handlers
     */
    public NettyRpcServer(int port, ServerSerializer serializer,
            Object... handlers) {
        this(null, port, serializer, 0L, handlers);
    }

    /**
     * tcpNoDelay: true, keepAlive: true
     * 
     * @param host
     * @param port
     * @param serializer
     * @param readTimeout
     * @param handlers
     */
    public NettyRpcServer(String host, int port, ServerSerializer serializer,
            long readTimeout, Object... handlers) {
        this(host, port, serializer, HandlerMapper.getHandlerMap(handlers),
                new SocketConfig("child.").setReuseAddress(true)
                        .setKeepAlive(true).setTcpNoDelay(true)
                        .setReadTimeout((int) readTimeout), new SocketConfig());
    }

    /**
     * @param host
     * @param port
     * @param serializer
     * @param handlers
     * @param childSocketOptions
     */
    public NettyRpcServer(String host, int port, ServerSerializer serializer,
            Map<String, Object> handlers, SocketConfig childSocketOptions) {
        this(host, port, serializer, handlers, childSocketOptions,
                new SocketConfig());
    }

    /**
     * @param host
     * @param port
     * @param ioTimeout
     * @param handlers
     *            key: interface qualified name, value: handler
     */
    public NettyRpcServer(String host, int port, ServerSerializer serializer,
            Map<String, Object> handlers, SocketConfig childSocketOptions,
            SocketConfig socketOptions) {
        if (null == serializer) {
            throw new NullPointerException("serializer");
        }
        if (null == handlers || handlers.size() == 0) {
            throw new IllegalArgumentException("handlers not provided");
        }
        if (null == childSocketOptions) {
            childSocketOptions = new SocketConfig("child.");
        }
        if (null == socketOptions) {
            socketOptions = new SocketConfig();
        }

        if (null == host) {
            this.inetAddr = new InetSocketAddress(port);
        } else {
            this.inetAddr = new InetSocketAddress(host, port);
        }
        this.serializer = serializer;
        this.handlersMap = handlers;
        this.socketOptions = socketOptions;
        this.childSocketOptions = childSocketOptions;
    }

    @Override
    public void start() throws Throwable {
        final ChannelGroup channelGroup = new DefaultChannelGroup(getClass()
                .getName());
        ChannelFactory channelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
        final Timer timer = new HashedWheelTimer();
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();

                int readTimeout = childSocketOptions.getReadTimeout();
                if (readTimeout > 0) {
                    pipeline.addLast("timeout", new ReadTimeoutHandler(timer,
                            readTimeout, TimeUnit.MILLISECONDS));
                }

                pipeline.addLast("decoder",
                        new InvokeRequestDecoder(serializer));
                pipeline.addLast("encoder", new InvokeResponseEncoder(
                        serializer));
                pipeline.addLast("handler", new NettyRpcServerHandler(
                        handlersMap, channelGroup));
                return pipeline;
            }
        });

        Map<String, Object> options = new HashMap<String, Object>();
        options.putAll(socketOptions.getOptions());
        options.putAll(childSocketOptions.getOptions());
        bootstrap.setOptions(options);

        Channel channel = bootstrap.bind(inetAddr);
        channelGroup.add(channel);
        waitForShutdownCommand();
        ChannelGroupFuture future = channelGroup.close();
        future.awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }

    @Override
    public void stop() throws Throwable {
        stopped.set(true);
        synchronized (stopped) {
            stopped.notifyAll();
        }
    }

    private void waitForShutdownCommand() {
        synchronized (stopped) {
            while (!stopped.get()) {
                try {
                    stopped.wait();
                } catch (InterruptedException e) {}
            }
        }
    }
}
