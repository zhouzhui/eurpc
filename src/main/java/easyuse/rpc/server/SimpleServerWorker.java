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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Map;

import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;
import easyuse.rpc.ServerSerializer;
import easyuse.rpc.util.IOUtils;
import easyuse.rpc.util.ReflectionCache;

/**
 * @author dhf
 */
public class SimpleServerWorker implements Runnable {
    private ServerSerializer serializer;

    private Map<String, Object> handlersMap;

    private Socket clientSocket;

    public SimpleServerWorker(ServerSerializer serializer,
            Map<String, Object> handlers, Socket clientSocket) {
        this.serializer = serializer;
        this.handlersMap = handlers;
        this.clientSocket = clientSocket;
    }

    private Object handle(InvokeRequest request) throws Throwable {
        Method method = ReflectionCache.getMethod(request.getClassName(),
                request.getMethodName(), request.getParameterTypes());
        Object[] parameters = request.getParameters();
        // get handler
        Object handler = handlersMap.get(request.getClassName());
        // invoke
        Object result = method.invoke(handler, parameters);
        return result;
    }

    @Override
    public void run() {
        Object result = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new BufferedInputStream(clientSocket.getInputStream());
            output = new BufferedOutputStream(clientSocket.getOutputStream());
            while (clientSocket.isConnected() && !clientSocket.isClosed()
                    && !clientSocket.isInputShutdown()
                    && !clientSocket.isOutputShutdown()) {
                InvokeRequest request = null;
                try {
                    request = serializer.decodeRequest(input);
                } catch (EOFException e) {
                    // client closed
                    return;
                }

                InvokeResponse response = new InvokeResponse();
                try {
                    result = handle(request);
                    response.setResult(result);
                } catch (Throwable t) {
                    printException(t);
                    response.setException(t);
                }
                serializer.encodeResponse(output, response);
                // flush response
                output.flush();
            }
        } catch (Throwable t) {
            printException(t);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(clientSocket);
        }
    }

    protected void printException(Throwable t) {
        t.printStackTrace();
    }
}
