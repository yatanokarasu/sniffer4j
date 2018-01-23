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


import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;


/**
 * Injects instrumentation code into byte-codes of methods included specified packages at the time
 * of JVM startup, see details for {@link java.lang.instrument.Instrumentation}.
 * 
 * <p>By default, Sniffer4j does NOT inject instrumentation code under the following packages:
 * {@link java}.*, {@link jdk}.*, {@link com.sun}.*, {@link sun}.*, {@link oracle}.* and {@link io.sniffer4j}.*.
 */
public final class Premain {

    /**
     * 
     * 
     * @param agentArguments Java Agent arguments
     * @param instrumentation An instrumentation instance for pre-main
     */
    public static void premain(final String agentArguments, final Instrumentation instrumentation) {
        parseArguments(agentArguments);

        LogBroker.instance().initialize();

        instrumentation.addTransformer(new Sniffer4jTransformer());
    }


    private static void parseArguments(final String agentArguments) {
        if (Objects.isNull(agentArguments)) {
            return;
        }

        separateByComma(agentArguments).forEach(setOptionValue());
    }


    private static Stream<String> separateByComma(final String agentArguments) {
        return Arrays.asList(agentArguments.split(",", -1)).stream();
    }


    private static Consumer<String> setOptionValue() {
        return arg -> {
            final String[] pair = arg.split("=", -1);
            final String name = pair[0];
            final String value = pair[1];

            try {
                Options.of(name).value(value);
            } catch (@SuppressWarnings("unused") final IllegalArgumentException ignored) {
                System.err.printf("Unexpected value '%s' of %s, use default value.", value, name);
            }
        };
    }

}
