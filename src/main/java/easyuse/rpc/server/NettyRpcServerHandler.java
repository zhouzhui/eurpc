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

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;

import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;
import easyuse.rpc.Logger;
import easyuse.rpc.util.LoggerHolder;
import easyuse.rpc.util.ReflectionCache;

/**
 * @author dhf
 */
public class NettyRpcServerHandler extends SimpleChannelUpstreamHandler {
    private static final Logger logger = LoggerHolder
            .getLogger(NettyRpcServerHandler.class);

    private final Map<String, Object> handlersMap;

    private final ChannelGroup channelGroups;

    public NettyRpcServerHandler(Map<String, Object> handlersMap) {
        this(handlersMap, null);
    }

    public NettyRpcServerHandler(Map<String, Object> handlersMap,
            ChannelGroup channelGroups) {
        this.handlersMap = handlersMap;
        this.channelGroups = channelGroups;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        if (null != channelGroups) {
            channelGroups.add(e.getChannel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        InvokeRequest request = (InvokeRequest) ctx.getAttachment();
        logger.warn("handle rpc request fail! request: <{}>", new Object[] {
            request
        }, e.getCause());
        e.getChannel().close().awaitUninterruptibly();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        if (!(msg instanceof InvokeRequest)) {
            return;
        }
        InvokeRequest request = (InvokeRequest) msg;
        ctx.setAttachment(request);

        InvokeResponse response = new InvokeResponse(request.getRequestID());
        try {
            Object result = handle(request);
            response.setResult(result);
        } catch (Throwable t) {
            logger.warn("handle rpc request fail! request: <{}>", new Object[] {
                request
            }, t);
            response.setException(t);
        }
        e.getChannel().write(response);
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
}
