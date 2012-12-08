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

import java.util.HashMap;
import java.util.Map;

/**
 * socket options
 * 
 * @author dhf
 */
public class SocketConfig {
    private Map<String, Object> options;

    private String prefix = "";

    public static final Integer RECEIVE_BUFFER_SIZE = Integer.valueOf(16384);

    public static final Integer SEND_BUFFER_SIZE = Integer.valueOf(16384);

    public static final Boolean TCP_NO_DELAY = Boolean.FALSE;

    public static final Boolean KEEP_ALIVE = Boolean.FALSE;

    public static final Boolean REUSE_ADDR = Boolean.FALSE;

    public static final Integer SO_LINGER = Integer.valueOf(-1);

    public static final Integer CONNECT_TIMEOUT = Integer.valueOf(0);

    public static final Integer READ_TIMEOUT = Integer.valueOf(0);

    public static final Integer TRAFFIC_CLASS = Integer.valueOf(0);

    public SocketConfig() {
        this("", null);
    }

    public SocketConfig(String keyPrefix) {
        this(keyPrefix, null);
    }

    public SocketConfig(Map<String, Object> options) {
        this("", options);
    }

    public SocketConfig(String keyPrefix, Map<String, Object> options) {
        this.prefix = keyPrefix;
        this.options = options;
        if (null == this.prefix) {
            this.prefix = "";
        }
        if (null == this.options) {
            this.options = new HashMap<String, Object>();
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getReceiveBufferSize() {
        return (Integer) get("receiveBufferSize", RECEIVE_BUFFER_SIZE);
    }

    public SocketConfig setReceiveBufferSize(int receiveBufferSize) {
        return set("receiveBufferSize", receiveBufferSize);
    }

    public int getSendBufferSize() {
        return (Integer) get("sendBufferSize", SEND_BUFFER_SIZE);
    }

    public SocketConfig setSendBufferSize(int sendBufferSize) {
        return set("sendBufferSize", sendBufferSize);
    }

    public boolean isTcpNoDelay() {
        return (Boolean) get("tcpNoDelay", TCP_NO_DELAY);
    }

    public SocketConfig setTcpNoDelay(boolean tcpNoDelay) {
        return set("tcpNoDelay", tcpNoDelay);
    }

    public boolean isKeepAlive() {
        return (Boolean) get("keepAlive", KEEP_ALIVE);
    }

    public SocketConfig setKeepAlive(boolean keepAlive) {
        return set("keepAlive", keepAlive);
    }

    public boolean isReuseAddress() {
        return (Boolean) get("reuseAddress", REUSE_ADDR);
    }

    public SocketConfig setReuseAddress(boolean reuseAddress) {
        return set("reuseAddress", reuseAddress);
    }

    public int getSoLinger() {
        return (Integer) get("soLinger", SO_LINGER);
    }

    public SocketConfig setSoLinger(int soLinger) {
        return set("soLinger", soLinger);
    }

    public int getTrafficClass() {
        return (Integer) get("trafficClass", TRAFFIC_CLASS);
    }

    public SocketConfig setTrafficClass(int trafficClass) {
        return set("trafficClass", trafficClass);
    }

    public int getConnectTimeout() {
        return (Integer) get("connectTimeoutMillis", CONNECT_TIMEOUT);
    }

    public SocketConfig setConnectTimeout(int connectTimeoutMillis) {
        return set("connectTimeoutMillis", connectTimeoutMillis);
    }

    public int getReadTimeout() {
        return (Integer) get("readTimeoutMillis", READ_TIMEOUT);
    }

    public SocketConfig setReadTimeout(int readTimeoutMillis) {
        return set("readTimeoutMillis", readTimeoutMillis);
    }

    private Object get(String key, Object defaultValue) {
        Object result = options.get(prefix + key);
        if (null == result) {
            result = defaultValue;
        }
        return result;
    }

    private SocketConfig set(String key, Object value) {
        options.put(prefix + key, value);
        return this;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public SocketConfig setOptions(Map<String, Object> options) {
        this.options = options;
        if (null == this.options) {
            this.options = new HashMap<String, Object>();
        }
        return this;
    }
}
