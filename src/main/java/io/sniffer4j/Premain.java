/*  
 *  The MIT License (MIT)
 *  
 *  Copyright (c) 2017 Yusuke TAKEI.
 *  
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.sniffer4j;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;


/**
 * 
 */
public final class Premain {
    
    private static final ClassPool CLASS_POOL = ClassPool.getDefault();
    
    
    /**
     * @param agentArguments Java Agent arguments
     * @param instrumentation An instrumentation instance for pre-main
     */
    public static void premain(final String agentArguments, final Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            
            /**
             * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader,
             *      java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
             */
            @Override
            public byte[] transform(ClassLoader loader, String fullyClassname, Class<?> arg3, ProtectionDomain protectionDomain, byte[] classfileByteSequence)
                throws IllegalClassFormatException {
                if (!accept(fullyClassname)) {
                    return null;
                }
                
                try (final InputStream byteStream = new ByteArrayInputStream(classfileByteSequence)) {
                    CtClass ctClass = CLASS_POOL.makeClass(byteStream);
                    CtMethod[] ctMethods = ctClass.getDeclaredMethods();
                    
                    final String className = ctClass.getName();
                    
                    for (final CtMethod aMethod : ctMethods) {
                        final String methodName = aMethod.getName();
                        final String varName = methodName + "_sniff_start";
                        //aMethod.addLocalVariable(varName, CtClass.);
                        //aMethod.insertBefore(varName + " = System.nanoTime();");
                        //aMethod.insertAfter("System.out.println(\"" + className + "#" + methodName
                        //    + "()[Thread=\" + Thread.currentThread().getName() + \"]: \" + (System.nanoTime() - " + varName + "));");
                        aMethod.addLocalVariable(varName, CLASS_POOL.getCtClass("java.time.Instant"));
                        aMethod.insertBefore(varName + " = java.time.Instant.now();");
                        aMethod.insertAfter("System.out.println(\"" + className + "#" + methodName
                            + "()[Thread=\" + Thread.currentThread().getName() + \":\" + Thread.currentThread().getId() + \"]: \" + java.time.Duration.between(" + varName + ", java.time.Instant.now()).toMillis());");
                        
                    }
                    
                    return ctClass.toBytecode();
                } catch (IOException | CannotCompileException | NotFoundException cause) {
                    IllegalClassFormatException e = new IllegalClassFormatException();
                    e.initCause(cause);
                    System.out.println(e);
                    throw e;
                }
            }
            
            
            private boolean accept(String fullyClassname) {
                return fullyClassname.startsWith("io");
            }
        });
    }
    
}
