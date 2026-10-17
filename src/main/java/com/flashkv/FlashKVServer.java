package com.flashkv;

import com.flashkv.server.NioServer;
import java.io.IOException;

public class FlashKVServer {
    private static final int DEFAULT_PORT = 6379;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("⚡ FlashKV Engine v1.0.0 — Pure Java Systems Core");
        System.out.println("==================================================");

        NioServer server = new NioServer(DEFAULT_PORT);

        // Graceful shutdown hook for Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n Shutting down FlashKV server...");
            server.stop();
        }));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Fatal: Could not start server: " + e.getMessage());
        }
    }
}