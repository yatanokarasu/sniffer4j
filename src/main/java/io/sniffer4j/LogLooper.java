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


import static java.nio.file.StandardOpenOption.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * 
 */
public class LogLooper {
    
    private static final BlockingQueue<Record> queue = new LinkedBlockingQueue<>();
    
    private final ExecutorService              executor;
    
    private static final class Holder {
        
        private static final LogLooper INSTANCE = new LogLooper();
        
    }
    
    
    private LogLooper() {
        this.executor = Executors.newSingleThreadExecutor();

        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            
            /**
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
                final ExecutorService executor = LogLooper.this.executor;
                executor.shutdown();
                
                try {
                    executor.awaitTermination(5_000, TimeUnit.SECONDS);
                } catch (@SuppressWarnings("unused") InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }

                if (executor.isTerminated()) {
                    executor.shutdownNow();
                }
            }
        });
    }
    
    
    public static void init() {
        Holder.INSTANCE.run();
    }
    
    
    private void run() {
        this.executor.execute(() -> {
            try (final BufferedWriter buffer = Files.newBufferedWriter(Paths.get("d:", "sniffer4j.log"), CREATE, TRUNCATE_EXISTING);
                final PrintWriter writer = new PrintWriter(buffer)) {
                writer.println(header());
                
                loopUntilInterrupt(writer);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
    }
    
    
    /**
     * 
     */
    private void loopUntilInterrupt(final PrintWriter writer) throws IOException {
        while (true) {
            try {
                Record aRecord = queue.take();
                
                writer.println(aRecord.toString());
                writer.flush();
            } catch (@SuppressWarnings("unused") InterruptedException exception) {
                Thread.currentThread().interrupt();
                
                break;
            }
        }
    }


    private String header() {
        return "thread_name,thread_id,start_time,end_time,time_taken";
    }
    
    
    /**
     * @param currentThread
     * @param start
     * @param end
     */
    public static void submit(Thread currentThread, Instant start, Instant end) {
        final Record aRecord = new Record();
        aRecord.threadName = currentThread.getName();
        aRecord.threadId = currentThread.getId();
        aRecord.start = start;
        aRecord.end = end;
        
        queue.offer(aRecord);
    }
    
    private static final class Record {
        
        private Instant start;
        
        private Instant end;
        
        private long    threadId;
        
        private String  threadName;
        
        
        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return new StringBuilder(this.threadName()).append(",")
                .append(this.threadId()).append(",")
                .append(toLocalDateTime(this.start)).append(",")
                .append(toLocalDateTime(this.end)).append(",")
                .append(duration())
                .toString();
        }
        
        
        private String threadName() {
            return this.threadName;
        }
        
        
        private String threadId() {
            return String.valueOf(this.threadId);
        }
        
        
        private String toLocalDateTime(final Instant anInstant) {
            return LocalDateTime.ofInstant(anInstant, ZoneId.systemDefault()).toString();
        }
        
        
        private String duration() {
            return String.valueOf(Duration.between(this.start, this.end).toMillis());
        }
    }
    
}
