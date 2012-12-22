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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dhf
 */
public class ReflectionCache {
    private static final Map<String, Class<?>> PRIMITIVE_CLASS = new HashMap<String, Class<?>>();

    private static final LRUMap<String, Class<?>> CLASS_CACHE = new LRUMap<String, Class<?>>(
            128);

    private static final LRUMap<String, Method> METHOD_CACHE = new LRUMap<String, Method>(
            1024);
    static {
        PRIMITIVE_CLASS.put("boolean", boolean.class);
        PRIMITIVE_CLASS.put("byte", byte.class);
        PRIMITIVE_CLASS.put("short", short.class);
        PRIMITIVE_CLASS.put("int", int.class);
        PRIMITIVE_CLASS.put("long", long.class);
        PRIMITIVE_CLASS.put("long", long.class);
        PRIMITIVE_CLASS.put("float", float.class);
        PRIMITIVE_CLASS.put("double", double.class);
        PRIMITIVE_CLASS.put("void", void.class);

        CLASS_CACHE.putAll(PRIMITIVE_CLASS);
    }

    public static Class<?> getClass(String className)
            throws ClassNotFoundException {
        Class<?> clazz = CLASS_CACHE.get(className);
        if (null != clazz) {
            return clazz;
        }
        synchronized (CLASS_CACHE) {
            if (null == CLASS_CACHE.get(className)) {
                clazz = PRIMITIVE_CLASS.get(className);
                if (null == clazz) {
                    clazz = Class.forName(className);
                }
                CLASS_CACHE.put(className, clazz);
                return clazz;
            } else {
                return CLASS_CACHE.get(className);
            }
        }
    }

    public static Method getMethod(String className, String methodName,
            String[] parameterTypes) throws ClassNotFoundException,
            SecurityException, NoSuchMethodException {
        String key = className + "-" + methodName + "-"
                + join(parameterTypes, ";");
        Method method = METHOD_CACHE.get(key);
        if (null != method) {
            return method;
        }
        synchronized (METHOD_CACHE) {
            if (null == METHOD_CACHE.get(key)) {
                Class<?> clazz = getClass(className);
                Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];
                for (int i = 0; i < parameterClasses.length; i++) {
                    parameterClasses[i] = getClass(parameterTypes[i]);
                }

                method = clazz.getMethod(methodName, parameterClasses);
                METHOD_CACHE.put(key, method);
                return method;
            } else {
                return METHOD_CACHE.get(key);
            }
        }
    }

    private static String join(String[] strs, String seperator) {
        if (null == strs || 0 == strs.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder(1024);
        sb.append(strs[0]);
        for (int i = 1; i < strs.length; i++) {
            sb.append(seperator).append(strs[i]);
        }
        return sb.toString();
    }
}
