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
package easyuse.rpc.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.Schema;

import easyuse.rpc.ClientSerializer;
import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;
import easyuse.rpc.SerializeException;
import easyuse.rpc.ServerSerializer;
import easyuse.rpc.util.BufferCache;
import easyuse.rpc.util.IOUtils;
import easyuse.rpc.util.SchemaCache;

/**
 * <p>
 * Format:
 * 
 * <pre>
 *  +---------------------------+-----------+
 *  |  content length(4 bytes)  |  content  |
 *  +---------------------------+-----------+
 * </pre>
 * 
 * @see <a href="http://code.google.com/p/protostuff/wiki/ProtostuffRuntime"
 *      >ProtostuffRuntime</a>
 * @author dhf
 */
public abstract class AbstractProtostuffSerializer implements ClientSerializer,
        ServerSerializer {
    /**
     * @param buffer
     *            buffer writen to
     * @param object
     * @param schema
     * @return length
     */
    protected abstract <T> int writeObject(LinkedBuffer buffer, T object,
            Schema<T> schema);

    /**
     * @param bytes
     * @param template
     * @param schema
     * @return
     */
    protected abstract <T> void parseObject(byte[] bytes, T template,
            Schema<T> schema);

    @Override
    public InvokeRequest decodeRequest(InputStream inputStream)
            throws SerializeException, IOException {
        return decode(inputStream, new InvokeRequest());
    }

    @Override
    public void encodeResponse(OutputStream outputStream, InvokeResponse result)
            throws SerializeException, IOException {
        encode(outputStream, result);
    }

    @Override
    public InvokeResponse decodeResponse(InputStream inputStream)
            throws SerializeException, IOException {
        return decode(inputStream, new InvokeResponse());
    }

    @Override
    public void encodeRequest(OutputStream outputStream, InvokeRequest request)
            throws SerializeException, IOException {
        encode(outputStream, request);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> void encode(OutputStream out, T object) throws IOException {
        LinkedBuffer buffer = BufferCache.getBuffer();
        Schema schema = null;
        if (null == object) {
            schema = SchemaCache.getSchema(Object.class);
        } else {
            schema = SchemaCache.getSchema(object.getClass());
        }

        // write the length header
        int length = writeObject(buffer, object, schema);
        IOUtils.writeInt(out, length);
        // write content
        LinkedBuffer.writeTo(out, buffer);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> T decode(InputStream in, T template) throws IOException {
        Schema schema = SchemaCache.getSchema(template.getClass());

        // read the length header
        int length = IOUtils.readInt(in);
        // read exactly $length bytes
        byte[] bytes = new byte[length];
        IOUtils.readFully(in, bytes, 0, length);
        // parse object
        parseObject(bytes, template, schema);
        return template;
    }
}
