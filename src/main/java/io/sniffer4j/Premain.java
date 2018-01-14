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
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * 
 */
public final class Premain {

    /**
     * @param agentArguments Java Agent arguments
     * @param instrumentation An instrumentation instance for pre-main
     */
    public static void premain(final String agentArguments, final Instrumentation instrumentation) {
        parseArguments(agentArguments);

        LogBroker.instance().initialize();

        instrumentation.addTransformer(new Sniffer4jTransformer());
    }


    private static void parseArguments(final String agentArguments) {
        Arrays.asList(agentArguments.split(",", -1)).stream()
            .map(toKeyValuePair())
            .filter(isValidOption())
            .map(keyToOptions())
            .forEach(setOptionValue());
    }


    private static Function<String, Map.Entry<String, String>> toKeyValuePair() {
        return v -> {
            final String[] pair = v.split("=", -1);

            return new AbstractMap.SimpleImmutableEntry<>(pair[0], pair[1]);
        };
    }


    private static Predicate<Map.Entry<String, String>> isValidOption() {
        return p -> {
            final String name = p.getKey();

            // validate option name
            try {
                Options.valueOf(name.toUpperCase());
            } catch (@SuppressWarnings("unused") final IllegalArgumentException ignored) {
                System.err.println("[WARNING] " + name + ": unknown option.");

                return false;
            }

            // validate option value
            if (p.getValue().isEmpty()) {
                System.err.println("[WARNING] " + name + ": invalid option.");

                return false;
            }

            return true;
        };
    }


    @SuppressWarnings("rawtypes")
    private static Function<Map.Entry<String, String>, Map.Entry<Options, String>> keyToOptions() {
        return v -> new AbstractMap.SimpleImmutableEntry<>(Options.valueOf(v.getKey()), v.getValue());
    }


    @SuppressWarnings("rawtypes")
    private static Consumer<Map.Entry<Options, String>> setOptionValue() {
        return e -> e.getKey().update(e.getValue());
    }

}
