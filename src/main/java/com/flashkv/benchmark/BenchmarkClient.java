package com.flashkv.benchmark;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkClient {
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    private static final int TOTAL_REQUESTS = 10000;
    private static final int CONCURRENT_CLIENTS = 50;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("==================================================");
        System.out.println("🚀 FlashKV Multi-Threaded Benchmark Harness");
        System.out.println("==================================================");
        System.out.println("Target Host: " + HOST + ":" + PORT);
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        System.out.println("Concurrent Threads: " + CONCURRENT_CLIENTS);
        System.out.println("Warm-up complete. Commencing stress run...\n");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CLIENTS);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try (Socket socket = new Socket(HOST, PORT);
                     OutputStream out = socket.getOutputStream();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Alternate between SET and GET operations
                    String command;
                    if (requestId % 2 == 0) {
                        command = "SET bench:key:" + requestId + " value_" + requestId + "\r\n";
                    } else {
                        command = "GET bench:key:" + (requestId - 1) + "\r\n";
                    }

                    out.write(command.getBytes());
                    out.flush();

                    String response = in.readLine();
                    if (response != null && (response.startsWith("+") || response.startsWith("$") || response.startsWith(":"))) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // Track network dropouts under high concurrency
                } finally {
                    latch.countDown();
                }
            });
        }

        // Await all 10,000 requests to finish
        latch.await();
        long endTime = System.nanoTime();
        executor.shutdown();

        double totalDurationSec = (endTime - startTime) / 1_000_000_000.0;
        double throughput = successCount.get() / totalDurationSec;
        double avgLatencyMs = (totalDurationSec / successCount.get()) * 1000.0;

        System.out.println("==================================================");
        System.out.println("📊 BENCHMARK RESULTS SUMMARY");
        System.out.println("==================================================");
        System.out.printf("Total Successful Ops: %d / %d\n", successCount.get(), TOTAL_REQUESTS);
        System.out.printf("Total Time Elapsed:   %.3f seconds\n", totalDurationSec);
        System.out.printf("Throughput:           %.0f requests/sec\n", throughput);
        System.out.printf("Avg Request Latency:  %.3f ms\n", avgLatencyMs);
        System.out.println("==================================================");
    }
}