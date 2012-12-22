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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import easyuse.rpc.ClientSerializer;
import easyuse.rpc.InvokeRequest;
import easyuse.rpc.InvokeResponse;
import easyuse.rpc.SerializeException;
import easyuse.rpc.ServerSerializer;
import easyuse.rpc.util.IOUtils;

/**
 * jdk default serialize
 * 
 * @author dhf
 */
public class JDKObjectSerializer implements ClientSerializer, ServerSerializer {

    private static final JDKObjectSerializer INSTANCE = new JDKObjectSerializer();

    private JDKObjectSerializer() {}

    public static JDKObjectSerializer getInstance() {
        return INSTANCE;
    }

    @Override
    public InvokeResponse decodeResponse(InputStream inputStream)
            throws SerializeException, IOException {
        try {
            // read length field header
            IOUtils.readInt(inputStream);
            // pase object
            ObjectInputStream input = new ObjectInputStream(inputStream);
            return (InvokeResponse) input.readObject();
        } catch (ClassNotFoundException e) {
            throw new SerializeException(e);
        }
    }

    @Override
    public void encodeRequest(OutputStream outputStream, InvokeRequest request)
            throws SerializeException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);
        ObjectOutputStream objOut = new ObjectOutputStream(baos);
        objOut.writeUTF(request.getRequestID());
        objOut.writeUTF(request.getClassName());
        objOut.writeUTF(request.getMethodName());
        objOut.writeObject(request.getParameterTypes());
        objOut.writeObject(request.getParameters());

        // get serialized object bytes
        byte[] bytes = baos.toByteArray();
        // write length field header
        IOUtils.writeInt(outputStream, bytes.length);
        // write object bytes
        outputStream.write(bytes);
    }

    public InvokeRequest decodeRequest(InputStream inputStream)
            throws SerializeException, IOException {
        try {
            // read length field header
            IOUtils.readInt(inputStream);
            // parse object
            ObjectInputStream input = new ObjectInputStream(inputStream);
            String requestID = input.readUTF();
            String className = input.readUTF();
            String methodName = input.readUTF();
            String[] parameterTypes = (String[]) input.readObject();
            Object[] parameters = (Object[]) input.readObject();
            return new InvokeRequest(requestID, className, methodName,
                    parameterTypes, parameters);
        } catch (ClassNotFoundException e) {
            throw new SerializeException(e);
        }
    }

    @Override
    public void encodeResponse(OutputStream outputStream, InvokeResponse result)
            throws SerializeException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);
        ObjectOutputStream objOut = new ObjectOutputStream(baos);
        objOut.writeObject(result);

        // get serialized object bytes
        byte[] bytes = baos.toByteArray();
        // write length field header
        IOUtils.writeInt(outputStream, bytes.length);
        // write object bytes
        outputStream.write(bytes);
    }
}
