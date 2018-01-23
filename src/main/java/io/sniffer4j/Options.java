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


import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;


/**
 * Defines options used by Sniffer4j.
 * 
 * <p>When instanciate this class, the following arguments is mandatory:
 * <dl>
 * <dt><code>defaultValue</code></dt>
 * <dd>Default value of the option</dd>
 * 
 * <dt><code>converter</code></dt>
 * <dd>{@link Function} to convert from String to Parameterized type</dd>
 * </dl>
 * 
 * <p>Also, you can specify the following arguments as optional:
 * <dl>
 * <dt><code>prehook</code></dt>
 * <dd>{@link UnaryOperator} for converting new value to desired format like
 * <code>"*" -> ".*"</code> in Regex</dd>
 * <dt><code>composer</code></dt>
 * <dd>{@link BiFunction} for composing or replacing default value and new value</dd>
 * </dl>
 */
class Options<L> {

    static final Options<Path>              LOGFILE   = Options.<Path> builder()
        .defaultValue(Paths.get(".", "sniffer4j.log"))
        .converter(v -> {
         // @formatter:off
            final Path logPath = Paths.get(v);
            final Path parentDir = logPath.getParent();
            
            try {
                Files.createDirectories(parentDir);
                Files.createFile(logPath);
            } catch (final IOException cause) {
                throw new UncheckedIOException(cause);
            }
            
            return logPath;
        })// @formatter:on
        .build();

    static final Options<Predicate<String>> PACKAGES  = Options.<Predicate<String>> builder()
        .defaultValue(Pattern.compile("^(javax?|jdk|(com/|)(sun|oracle)|io/sniffer4j).*").asPredicate())
        .converter(v -> Pattern.compile(v).asPredicate())
        .withOptional()
        .prehook(v -> v.replace(";", "|").replace(".", "/").replaceAll("*", Pattern.quote("*")))
        .composer((o, n) -> o.or(n.negate()))
        .build();

    @SuppressWarnings("boxing")
    static final Options<Integer>           INTERVAL  = new IntValueOptions(Integer.MIN_VALUE);

    @SuppressWarnings("boxing")
    static final Options<Integer>           THRESHOLD = new IntValueOptions(Integer.MIN_VALUE);

    static final Options<Void>              NULL      = new NullOptions();

    private final BiFunction<L, L, L>       composer;

    private final Function<String, L>       converter;

    private final Function<String, String>  prehook;

    private L                               value;


    private Options(final L defaultValue, final Function<String, L> converter) {
        this(defaultValue, converter, UnaryOperator.identity(), (o, n) -> n);
    }


    private Options(final L defaultValue, final Function<String, L> converter, final UnaryOperator<String> prehook, final BiFunction<L, L, L> composer) {
        this.value = defaultValue;
        this.converter = converter;
        this.prehook = prehook;
        this.composer = composer;
    }


    private static <L> MandatoryValueBuilder<L> builder() {
        return new MandatoryValueBuilder<>();
    }


    static Options<?> of(final String name) {
        switch (name.toUpperCase()) {
        // @formatter:off
        case "PACKAGES":  return PACKAGES;
        case "THRESHOLD": return THRESHOLD;
        case "LOGFILE":   return LOGFILE;
        // @formatter:on
        default:
            System.err.println("No option: " + name + ".");
            return NULL;
        }
    }


    void value(final String newValue) {
        try {
            L converted = this.converter.apply(this.prehook.apply(newValue));
            this.value = this.composer.apply(this.value, converted);
        } catch (final Throwable cause) {
            throw new IllegalArgumentException(cause);
        }
    }


    L value() {
        return this.value;
    }


    private static final class IntValueOptions extends Options<Integer> {

        private IntValueOptions(final Integer defaultValue) {
            super(defaultValue, Integer::valueOf);
        }

    }


    private static final class MandatoryValueBuilder<L> {

        private Function<String, L> converter;

        private L                   defaultValue;


        private Options<L> build() {
            validate();

            return new Options<>(this.defaultValue, this.converter);
        }


        private MandatoryValueBuilder<L> converter(final Function<String, L> converter) {
            this.converter = converter;
            return this;
        }


        private MandatoryValueBuilder<L> defaultValue(final L defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }


        private void validate() {
            if (Objects.isNull(this.defaultValue) || Objects.isNull(this.converter)) {
                throw new IllegalStateException();
            }
        }


        private OptionalValueBuilder<L> withOptional() {
            validate();

            return new OptionalValueBuilder<>(this.defaultValue, this.converter);
        }

    }


    private static final class NullOptions extends Options<Void> {

        private NullOptions() {
            super(null, null);
        }

    }


    private static final class OptionalValueBuilder<L> {

        private final L                   defaultValue;

        private final Function<String, L> converter;

        private UnaryOperator<String>     prehook;

        private BiFunction<L, L, L>       composer;


        private OptionalValueBuilder(final L defaultValue, final Function<String, L> converter) {
            this.defaultValue = defaultValue;
            this.converter = converter;
            this.prehook = UnaryOperator.identity();
            this.composer = (o, n) -> n;
        }


        private Options<L> build() {
            return new Options<>(this.defaultValue, this.converter, this.prehook, this.composer);
        }


        private OptionalValueBuilder<L> composer(final BiFunction<L, L, L> composer) {
            this.composer = composer;
            return this;
        }


        private OptionalValueBuilder<L> prehook(final UnaryOperator<String> prehook) {
            this.prehook = prehook;
            return this;
        }

    }

}
