/*  
 *  The MIT License (MIT)
 *  
 *  Copyright (c) 2018 Yusuke TAKEI.
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
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;


/**
 * 
 */
final class Sniffer4jTransformer implements ClassFileTransformer {

    private static final ClassPool CLASS_POOL = ClassPool.getDefault();


    /**
     * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader,
     *      java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
     */
    @Override
    public byte[] transform(
        final ClassLoader loader,
        final String fullyClassname,
        final Class<?> classBeingRedefined,
        final ProtectionDomain protectionDomain,
        final byte[] classfileByteSequence) throws IllegalClassFormatException {
        if (isNotSubjectToBeInjectedSniffer(fullyClassname)) {
            return null;
        }

        try (final InputStream byteStream = new ByteArrayInputStream(classfileByteSequence)) {
            return injectSniffer(byteStream);
        } catch (final IOException | CannotCompileException | NotFoundException cause) {
            final IllegalClassFormatException e = new IllegalClassFormatException();
            e.initCause(cause);

            throw e;
        }

    }


    private boolean isNotSubjectToBeInjectedSniffer(final String fullyClassname) {
        return !Options.PATTERNS.handler().test(fullyClassname);
    }


    private byte[] injectSniffer(final InputStream byteStream) throws IOException, CannotCompileException, NotFoundException {
        final CtClass ctClass = CLASS_POOL.makeClass(byteStream);
        final String className = ctClass.getName();

        final CtMethod[] ctMethods = ctClass.getDeclaredMethods();

        for (final CtMethod aMethod : ctMethods) {
            injectPerMethod(className, aMethod);
        }

        return ctClass.toBytecode();
    }


    private void injectPerMethod(final String className, final CtMethod aMethod) throws CannotCompileException, NotFoundException {
        final String methodName = aMethod.getName();
        final String beginVariableName = "beginSniffer";
        final String endVariableName = "endSniffer";

        aMethod.addLocalVariable(beginVariableName, CLASS_POOL.getCtClass("java.time.Instant"));
        aMethod.addLocalVariable(endVariableName,   CLASS_POOL.getCtClass("java.time.Instant"));

        aMethod.insertBefore(beginVariableName + " = java.time.Instant.now();");
        aMethod.insertAfter(endVariableName    + " = java.time.Instant.now();");

        aMethod.insertAfter("System.out.println(\"" + className + "#" + methodName
            + "()[Thread=\" + Thread.currentThread().getName() + \":\" + Thread.currentThread().getId() + \"]: \" + java.time.Duration.between("
            //+ beginVariableName + ", java.time.Instant.now()).toMillis());");
            + beginVariableName + ", " + endVariableName + ").toMillis());");
    }

}
