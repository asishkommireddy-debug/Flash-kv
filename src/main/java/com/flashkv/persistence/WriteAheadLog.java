package com.flashkv.persistence;

import com.flashkv.storage.MemoryStore;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class WriteAheadLog {
    private final String logFilePath;
    private FileChannel fileChannel;

    public WriteAheadLog(String logFilePath) {
        this.logFilePath = logFilePath;
        openChannel();
    }

    private void openChannel() {
        try {
            // Open file in APPEND mode. Creates file if it doesn't exist.
            this.fileChannel = FileChannel.open(
                Paths.get(this.logFilePath),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("❌ Fatal: Failed to open Write-Ahead Log file: " + e.getMessage());
        }
    }

    // Appends a command to disk and forces OS fsync
    public synchronized void append(String command) {
        if (this.fileChannel == null || !this.fileChannel.isOpen()) {
            return;
        }

        try {
            String record = command.trim() + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(record.getBytes());

            while (buffer.hasRemaining()) {
                this.fileChannel.write(buffer);
            }

            // CRITICAL: Forces the Linux kernel to flush dirty pages from RAM to physical disk (fsync)
            this.fileChannel.force(false);

        } catch (IOException e) {
            System.err.println("❌ WAL write error: " + e.getMessage());
        }
    }

    // Replays log on startup to recover in-memory state
    public void recover(MemoryStore store) {
        File logFile = new File(this.logFilePath);
        if (!logFile.exists() || logFile.length() == 0) {
            System.out.println("📁 No existing WAL file found. Starting with clean memory state.");
            return;
        }

        System.out.println("🔄 [Recovery] Replaying Write-Ahead Log from " + this.logFilePath + "...");
        int replayedCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                String command = parts[0].toUpperCase();

                if ("SET".equals(command) && parts.length >= 3) {
                    store.set(parts[1], parts[2]);
                    replayedCount++;
                } else if ("DEL".equals(command) && parts.length >= 2) {
                    store.delete(parts[1]);
                    replayedCount++;
                }
            }
            System.out.println("✅ [Recovery Complete] Successfully replayed " + replayedCount + " transactions into RAM!");

        } catch (IOException e) {
            System.err.println("❌ Error replaying WAL file: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (this.fileChannel != null && this.fileChannel.isOpen()) {
                this.fileChannel.force(true);
                this.fileChannel.close();
            }
        } catch (IOException ignored) {}
    }
}