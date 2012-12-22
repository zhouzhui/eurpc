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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;

import easyuse.rpc.ClientSerializer;
import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;
import easyuse.rpc.RpcConnection;
import easyuse.rpc.util.IOUtils;
import easyuse.rpc.util.SocketConfig;

/**
 * block io rpc connection
 * 
 * @author dhf
 */
public class BIORpcConnection implements RpcConnection {
    private InetSocketAddress inetAddr;

    private ClientSerializer serializer;

    private SocketConfig socketOptions;

    private SocketFactory socketFactory;

    private Socket socket;

    private InputStream in;

    private OutputStream out;

    private boolean connected;

    /**
     * tcpNoDelay: true, keepAlive: true, connectTimeout: infinite, readTimeout:
     * infinite
     * 
     * @param host
     * @param port
     * @param serializer
     */
    public BIORpcConnection(String host, int port,
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
    public BIORpcConnection(String host, int port,
            ClientSerializer serializer, long connectTimeout, long readTimeout) {
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
    public BIORpcConnection(String host, int port,
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
    }

    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }
    
    public void setSocketOptions(SocketConfig socketOptions) {
        this.socketOptions = socketOptions;
    }

    public void connect() throws Throwable {
        if (connected) {
            return;
        }
        if (null != socketFactory) {
            socket = socketFactory.createSocket();
        } else {
            socket = new Socket();
        }
        IOUtils.setSocketOptions(socket, socketOptions).connect(inetAddr,
                socketOptions.getConnectTimeout());
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
        connected = true;
    }

    @Override
    public void close() throws Throwable {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(socket);

        in = null;
        out = null;
        socket = null;
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isClosed() {
        return (null == socket) || !socket.isConnected() || socket.isClosed()
                || socket.isInputShutdown() || socket.isOutputShutdown();
    }

    @Override
    public InvokeResponse sendRequest(InvokeRequest request) throws Throwable {
        if (!isConnected()) {
            throw new IllegalStateException("not connected");
        }
        serializer.encodeRequest(out, request);
        out.flush();
        InvokeResponse response = serializer.decodeResponse(in);
        return response;
    }

}
