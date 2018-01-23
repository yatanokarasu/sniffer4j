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


import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * 
 */
public class LogBroker {

    private final BlockingQueue<Record> queue = new LinkedBlockingQueue<>(1_024);

    private final ExecutorService       consumer;


    private LogBroker() {
        this.consumer = Executors.newSingleThreadExecutor(this::newDaemonThread);
    }


    /**
     * @return A singleton instance
     */
    public static LogBroker instance() {
        return SingletonHolder.SINGLETON;
    }


    /**
     * @param aThread An instance of {@link Thread} that called this method
     * @param className Name of class which called this method
     * @param methodName Name of method which called this method
     * @param begin An instant at the beginning of method execution
     * @param end An instant at the end of method execution
     */
    public void submit(final Thread aThread, final String className, final String methodName, final Instant begin, final Instant end) {
        final Record aRecord = new Record();
        aRecord.threadName = aThread.getName();
        aRecord.threadId = aThread.getId();
        aRecord.className = className;
        aRecord.methodName = methodName;
        aRecord.begin = begin;
        aRecord.end = end;

        this.queue.offer(aRecord);
    }


    /**
     * 
     */
    void initialize() {
        startConsumerThread();
        registerShutdownHookForConsumerThread();
    }


    private String csvFileHeader() {
        return "thread_name,thread_id,begin_time,end_time,time_taken";
    }


    private Thread newDaemonThread(final Runnable runnable) {
        final Thread thread = new Thread(runnable);
        thread.setDaemon(true);

        return thread;
    }


    private void loopUntilInterrupt(final PrintWriter writer) {
        while (true) {
            try {
                Record aRecord = this.queue.take();

                writer.println(aRecord.toString());
                writer.flush();
            } catch (@SuppressWarnings("unused") InterruptedException exception) {
                Thread.interrupted();

                break;
            }
        }
    }


    private void registerShutdownHookForConsumerThread() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                final ExecutorService consumer = LogBroker.this.consumer;
                consumer.shutdown();

                try {
                    consumer.awaitTermination(1_000, TimeUnit.MILLISECONDS);
                } catch (@SuppressWarnings("unused") InterruptedException exception) {
                    Thread.interrupted();
                }

                if (!consumer.isTerminated()) {
                    consumer.shutdownNow();
                }
            }

        });
    }


    private void startConsumerThread() {
        this.consumer.execute(() -> {
            try (final BufferedWriter buffer = Files.newBufferedWriter(Options.LOGFILE.value(), CREATE, TRUNCATE_EXISTING);
                final PrintWriter writer = new PrintWriter(buffer)) {
                writer.println(csvFileHeader());

                loopUntilInterrupt(writer);
            } catch (final IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
    }


    private static final class SingletonHolder {

        private static final LogBroker SINGLETON = new LogBroker();

    }


    private static final class Record {

        private Instant begin;

        private Instant end;

        private String  className;

        private String  methodName;

        private long    threadId;

        private String  threadName;


        private String begin() {
            return toLocalDateTime(this.begin);
        }


        private String end() {
            return toLocalDateTime(this.end);
        }


        private String duration() {
            return String.valueOf(Duration.between(this.begin, this.end).toMillis());
        }


        private String className() {
            return this.className;
        }


        private String methodName() {
            return this.methodName;
        }


        private String threadId() {
            return String.valueOf(this.threadId);
        }


        private String threadName() {
            return this.threadName;
        }


        private String toLocalDateTime(final Instant anInstant) {
            return LocalDateTime.ofInstant(anInstant, ZoneId.systemDefault()).toString();
        }


        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return new StringJoiner(",")
                .add(threadName())
                .add(threadId())
                .add(className())
                .add(methodName())
                .add(begin())
                .add(end())
                .add(duration())
                .toString();
        }
    }

}
