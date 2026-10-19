package com.flashkv.server;

import com.flashkv.storage.MemoryStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NioServer {
    private final int port;
    private final MemoryStore store;
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private boolean isRunning;

    public NioServer(int port) {
        this.port = port;
        this.store = new MemoryStore(3);
    }

    public void start() throws IOException {
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.bind(new InetSocketAddress("localhost", this.port));
        this.serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        this.isRunning = true;
        System.out.println("⚡ FlashKV Server listening on port " + this.port + " [NIO Event Loop Ready]");

        runEventLoop();
    }

    private void runEventLoop() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (this.isRunning) {
            try {
                this.selector.select();
                Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key, buffer);
                    }
                }
            } catch (IOException e) {
                System.err.println("Event loop error: " + e.getMessage());
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(this.selector, SelectionKey.OP_READ);
            System.out.println(" Client connected: " + clientChannel.getRemoteAddress());
        }
    }

    private void handleRead(SelectionKey key, ByteBuffer buffer) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        buffer.clear();

        try {
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                System.out.println(" Client disconnected: " + clientChannel.getRemoteAddress());
                clientChannel.close();
                key.cancel();
                return;
            }

            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            String message = new String(data).trim();
            System.out.println("📨 Received from client: [" + message + "]");
             
            String[] parts = message.split("\\s+");
String command = parts[0].toUpperCase();
String response;

switch (command) {
    case "PING":
        response = "+PONG\r\n";
        break;

    case "SET":
        if (parts.length < 3) {
            response = "-ERR wrong number of arguments for 'SET'\r\n";
        } else {
            this.store.set(parts[1], parts[2]);
            response = "+OK\r\n";
        }
        break;

    case "GET":
        if (parts.length < 2) {
            response = "-ERR wrong number of arguments for 'GET'\r\n";
        } else {
            String val = this.store.get(parts[1]);
            if (val == null) {
                response = "$-1\r\n"; // Nil / Not found in Redis protocol
            } else {
                response = "+" + val + "\r\n";
            }
        }
        break;

    case "DEL":
        if (parts.length < 2) {
            response = "-ERR wrong number of arguments for 'DEL'\r\n";
        } else {
            boolean deleted = this.store.delete(parts[1]);
            response = ":" + (deleted ? 1 : 0) + "\r\n";
        }
        break;

    case "EXPIRE":
        if (parts.length < 3) {
            response = "-ERR wrong number of arguments for 'EXPIRE'\r\n";
        } else {
            try {
                long seconds = Long.parseLong(parts[2]);
                boolean set = this.store.expire(parts[1], seconds);
                response = ":" + (set ? 1 : 0) + "\r\n";
            } catch (NumberFormatException e) {
                response = "-ERR value is not an integer or out of range\r\n";
            }
        }
        break;

    case "TTL":
        if (parts.length < 2) {
            response = "-ERR wrong number of arguments for 'TTL'\r\n";
        } else {
            long remaining = this.store.ttl(parts[1]);
            response = ":" + remaining + "\r\n";
        }
        break;

    default:
        response = "-ERR unknown command '" + command + "'\r\n";
        break;
}

clientChannel.write(ByteBuffer.wrap(response.getBytes()));


        
        } catch (IOException e) {
            System.err.println("Connection reset by client.");
            try {
                clientChannel.close();
            } catch (IOException ignored) {}
            key.cancel();
        }
    }

    public void stop() {
        this.isRunning = false;
        try {
            if (this.serverChannel != null) this.serverChannel.close();
            if (this.selector != null) this.selector.close();
        } catch (IOException ignored) {}
    }
}