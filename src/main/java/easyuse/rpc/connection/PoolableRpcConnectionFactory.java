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

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import easyuse.rpc.RpcConnection;
import easyuse.rpc.RpcConnectionFactory;

/**
 * @author dhf
 */
public class PoolableRpcConnectionFactory implements RpcConnectionFactory,
        PoolableObjectFactory<RpcConnection> {
    private RpcConnectionFactory connectionFactory;

    private GenericObjectPool<RpcConnection> pool = new GenericObjectPool<RpcConnection>(
            this);

    public PoolableRpcConnectionFactory(RpcConnectionFactory factory) {
        if (null == factory) {
            throw new NullPointerException("factory");
        }
        this.connectionFactory = factory;
    }

    @Override
    public RpcConnection getConnection() throws Throwable {
        return pool.borrowObject();
    }

    @Override
    public void recycle(RpcConnection connection) throws Throwable {
        if (null != connection) {
            pool.returnObject(connection);
        }
    }

    public void destroy() throws Throwable {
        pool.close();
    }

    @Override
    public void activateObject(RpcConnection connection) throws Exception {
        try {
            connection.connect();
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    @Override
    public void destroyObject(RpcConnection connection) throws Exception {
        try {
            connection.close();
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    @Override
    public RpcConnection makeObject() throws Exception {
        try {
            return connectionFactory.getConnection();
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    @Override
    public void passivateObject(RpcConnection connection) throws Exception {
        // do nothing.
    }

    @Override
    public boolean validateObject(RpcConnection connection) {
        return connection.isConnected() && !connection.isClosed();
    }

    public void setLifo(boolean lifo) {
        pool.setLifo(lifo);
    }

    public void setMaxActive(int maxActive) {
        pool.setMaxActive(maxActive);
    }

    public void setMaxIdle(int maxIdle) {
        pool.setMaxIdle(maxIdle);
    }

    public void setMaxWait(long maxWait) {
        pool.setMaxWait(maxWait);
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        pool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    public void setMinIdle(int minIdle) {
        pool.setMinIdle(minIdle);
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        pool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
    }

    public void setSoftMinEvictableIdleTimeMillis(
            long softMinEvictableIdleTimeMillis) {
        pool.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        pool.setTestOnBorrow(testOnBorrow);
    }

    public void setTestOnReturn(boolean testOnReturn) {
        pool.setTestOnReturn(testOnReturn);
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        pool.setTestWhileIdle(testWhileIdle);
    }

    public void setTimeBetweenEvictionRunsMillis(
            long timeBetweenEvictionRunsMillis) {
        pool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    public void setWhenExhaustedAction(byte whenExhaustedAction) {
        pool.setWhenExhaustedAction(whenExhaustedAction);
    }
}
