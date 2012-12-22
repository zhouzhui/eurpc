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

import java.net.InetSocketAddress;

import easyuse.rpc.ClientSerializer;
import easyuse.rpc.RpcConnection;
import easyuse.rpc.RpcConnectionFactory;
import easyuse.rpc.util.SocketConfig;

/**
 * @author dhf
 */
public class NettyRpcConnectionFactory implements RpcConnectionFactory {
    private InetSocketAddress serverAddr;

    private ClientSerializer serializer;

    private SocketConfig socketOptions;

    /**
     * tcpNoDelay: true, keepAlive: true, connectTimeout: infinite, readTimeout:
     * infinite
     * 
     * @param host
     * @param port
     * @param serializer
     */
    public NettyRpcConnectionFactory(String host, int port,
            ClientSerializer serializer) {
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
    public NettyRpcConnectionFactory(String host, int port,
            ClientSerializer serializer, long connectTimeout, long readTimeout) {
        this(host, port, serializer, new SocketConfig().setKeepAlive(true)
                .setTcpNoDelay(true).setConnectTimeout((int) connectTimeout)
                .setReadTimeout((int) readTimeout));
    }

    public NettyRpcConnectionFactory(String host, int port,
            ClientSerializer serializer, SocketConfig socketOptions) {
        if (null == serializer) {
            throw new NullPointerException("serializer");
        }
        if (null == socketOptions) {
            socketOptions = new SocketConfig();
        }

        this.serverAddr = new InetSocketAddress(host, port);
        this.serializer = serializer;
        this.socketOptions = socketOptions;
    }

    @Override
    public RpcConnection getConnection() throws Throwable {
        return new NettyRpcConnection(this.serverAddr.getHostName(),
                this.serverAddr.getPort(), this.serializer, this.socketOptions);
    }

    @Override
    public void recycle(RpcConnection connection) throws Throwable {
        if (null != connection) {
            connection.close();
        }
    }

}
