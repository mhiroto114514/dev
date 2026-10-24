package com.example.advisor.desktop;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class InstanceLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private InstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    static InstanceLock acquire(Path path) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock != null) return new InstanceLock(channel, lock);
        } catch (OverlappingFileLockException ignored) {
            // A second launcher in this JVM is also a duplicate instance.
        } catch (IOException | RuntimeException error) {
            channel.close();
            throw error;
        }
        channel.close();
        return null;
    }

    @Override public void close() throws IOException {
        try {
            lock.release();
        } finally {
            channel.close();
        }
        // Keep the file: deleting it can let another process lock a different file.
    }
}
