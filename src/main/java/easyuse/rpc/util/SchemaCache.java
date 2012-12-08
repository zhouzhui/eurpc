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

import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;

/**
 * @author dhf
 */
public class SchemaCache {
    private static final LRUMap<String, Schema<?>> SCHEMA_CACHE = new LRUMap<String, Schema<?>>(
            4096);

    @SuppressWarnings("unchecked")
    public static <T> Schema<T> getSchema(Class<T> clazz) {
        String className = clazz.getName();
        Schema<T> schema = (Schema<T>) SCHEMA_CACHE.get(className);
        if (null != schema) {
            return schema;
        }
        synchronized (SCHEMA_CACHE) {
            if (null == SCHEMA_CACHE.get(className)) {
                schema = RuntimeSchema.getSchema(clazz);
                SCHEMA_CACHE.put(className, schema);
                return schema;
            } else {
                return (Schema<T>) SCHEMA_CACHE.get(className);
            }
        }
    }

    public static Schema<InvokeRequest> getSchema(InvokeRequest request) {
        Schema<InvokeRequest> schema = getSchema(InvokeRequest.class);
        Object[] parameters = request.getParameters();
        if (null != parameters && parameters.length > 0) {
            for (Object param: parameters) {
                if (null != param) {
                    getSchema(param.getClass());
                }
            }
        }
        return schema;
    }

    public static Schema<InvokeResponse> getSchema(InvokeResponse response) {
        Schema<InvokeResponse> schema = getSchema(InvokeResponse.class);
        if (response.getException() != null) {
            getSchema(response.getException().getClass());
        }
        if (response.getResult() != null) {
            getSchema(response.getResult().getClass());
        }
        return schema;
    }
}
