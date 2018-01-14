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


import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;


/**
 * 
 */
class Options<L> {

    static final Options<Predicate<String>> PATTERNS = new Options<>(
        Pattern.compile("^(javax?|jdk|(com\\.|)(sun|oracle))").asPredicate().negate(),
        (defaultHandler, newValue) -> defaultHandler.and(Pattern.compile(newValue).asPredicate()),
        s -> s.replace(";", "|")
    );

    private final L                         defaultHandler;

    private final BiFunction<L, String, L>  updateHandler;
    
    private Function<String, String>        hook;

    private L                               handler;


    private Options(final L defaultHandler, final BiFunction<L, String, L> updateHandler) {
        this(defaultHandler, updateHandler, Function.identity());
    }
    
    
    private Options(final L defaultHandler, final BiFunction<L, String, L> updateHandler, final Function<String, String> hook) {
        this.defaultHandler = defaultHandler;
        this.updateHandler = updateHandler;
        this.hook = hook;
    }


    static Options<?> valueOf(final String name) {
        switch (name.toUpperCase()) {
        case "PATTERNS":
            return PATTERNS;
        default:
            throw new IllegalArgumentException("No constant " + name + ".");
        }
    }


    L handler() {
        return this.handler;
    }


    void update(final String value) {
        this.handler = this.updateHandler.apply(this.defaultHandler, this.hook.apply(value));
    }

}
