package com.example.advisor.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class InstanceLockTest {
    @TempDir Path directory;

    @Test void rejectsDuplicateAndAllowsRestartEvenWithExistingLockFile() throws Exception {
        Path path = directory.resolve("application.lock");
        try (InstanceLock first = InstanceLock.acquire(path)) {
            assertNotNull(first);
            assertNull(InstanceLock.acquire(path));
        }
        try (InstanceLock restarted = InstanceLock.acquire(path)) {
            assertNotNull(restarted);
        }
    }
}
