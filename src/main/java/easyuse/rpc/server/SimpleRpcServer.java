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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import easyuse.rpc.RpcServer;
import easyuse.rpc.ServerSerializer;
import easyuse.rpc.util.HandlerMapper;
import easyuse.rpc.util.IOUtils;
import easyuse.rpc.util.SocketConfig;

/**
 * a simple block io rpc server
 * 
 * @author dhf
 */
public class SimpleRpcServer implements RpcServer {
    protected InetSocketAddress inetAddr;

    protected Map<String, Object> handlerMap;

    private AtomicBoolean stopped = new AtomicBoolean(false);

    private ExecutorService executor = Executors.newCachedThreadPool();

    private ServerSocket server;

    private ServerSerializer serializer;

    private SocketConfig socketOptions;

    /**
     * tcpNoDelay: true, keepAlive: true, readTimeout: infinite
     * 
     * @param port
     * @param serializer
     * @param handlers
     */
    public SimpleRpcServer(int port, ServerSerializer serializer,
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
    public SimpleRpcServer(String host, int port, ServerSerializer serializer,
            long readTimeout, Object... handlers) {
        this(host, port, serializer, HandlerMapper.getHandlerMap(handlers),
                new SocketConfig().setReuseAddress(true).setKeepAlive(true)
                        .setTcpNoDelay(true).setReadTimeout((int) readTimeout));
    }

    /**
     * @param host
     * @param port
     * @param serializer
     * @param handlers
     *            key: interface qualified name, value: handler
     * @param socketOptions
     */
    public SimpleRpcServer(String host, int port, ServerSerializer serializer,
            Map<String, Object> handlers, SocketConfig socketOptions) {
        if (null == serializer) {
            throw new NullPointerException("serializer");
        }
        if (null == handlers || handlers.size() == 0) {
            throw new IllegalArgumentException("handlers not provided");
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
        this.handlerMap = handlers;
        this.socketOptions = socketOptions;
    }

    @Override
    public void start() throws Throwable {
        try {
            server = new ServerSocket();
            server.setReceiveBufferSize(socketOptions.getReceiveBufferSize());
            server.bind(inetAddr);
            while (!stopped.get()) {
                try {
                    Socket socket = server.accept();
                    if (!stopped.get()) {
                        IOUtils.setSocketOptions(socket, socketOptions);
                        executor.submit(getWorker(socket));
                    } else {
                        break;
                    }
                } catch (Throwable t) {}
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(socketOptions.getReadTimeout(),
                    TimeUnit.MICROSECONDS);
            server.close();
        }
    }

    @Override
    public void stop() throws Throwable {
        stopped.set(true);
        server.close();
    }

    protected SimpleServerWorker getWorker(Socket socket) {
        return new SimpleServerWorker(serializer, handlerMap, socket);
    }

}
