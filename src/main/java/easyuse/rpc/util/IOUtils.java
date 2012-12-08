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

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * @author dhf
 */
public class IOUtils {
    /**
     * block until exactly <code>length</code> bytes read into
     * <code>bytes</code>
     * 
     * @param in
     * @param bytes
     * @param offset
     * @param length
     * @throws IOException
     */
    public static void readFully(InputStream in, byte[] bytes, int offset,
            int length) throws IOException {
        if (length < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < length) {
            int count = in.read(bytes, offset + n, length - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
    }

    /**
     * write an integer to the output stream
     * 
     * @param out
     * @param value
     * @throws IOException
     */
    public static void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 0) & 0xFF);
    }

    /**
     * read an integer from the input stream
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public static void closeQuietly(Closeable closeable) {
        if (null == closeable) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable t) {}
    }

    public static void closeQuietly(Socket socket) {
        if (null == socket) {
            return;
        }
        if (!socket.isInputShutdown()) {
            try {
                socket.shutdownInput();
            } catch (IOException e) {}
        }
        if (!socket.isOutputShutdown()) {
            try {
                socket.shutdownOutput();
            } catch (IOException e) {}
        }
        try {
            socket.close();
        } catch (Throwable t) {}
    }

    /**
     * set socket options
     * 
     * @param socket
     * @param options
     * @return
     * @throws SocketException
     */
    public static Socket setSocketOptions(Socket socket, SocketConfig options)
            throws SocketException {
        if (null == options || options.getOptions().size() == 0) {
            return socket;
        }

        socket.setSoTimeout(options.getReadTimeout());
        socket.setReceiveBufferSize(options.getReceiveBufferSize());
        socket.setSendBufferSize(options.getSendBufferSize());
        socket.setTcpNoDelay(options.isTcpNoDelay());
        socket.setKeepAlive(options.isKeepAlive());
        socket.setReuseAddress(options.isReuseAddress());
        socket.setTrafficClass(options.getTrafficClass());

        if (options.getSoLinger() < 0) {
            socket.setSoLinger(false, 0);
        } else {
            socket.setSoLinger(true, options.getSoLinger());
        }
        return socket;
    }
}
