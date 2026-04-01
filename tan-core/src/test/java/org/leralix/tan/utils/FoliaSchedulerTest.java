package org.leralix.tan.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FoliaScheduler}.
 * <p>
 * Tests scheduling methods for Folia regionalized execution.
 * <p>
 * NOTE: These tests are disabled because they require a real Folia server
 * with proper regionalized scheduling. MockBukkit does not fully support
 * Folia's scheduler API. These tests should be run as integration tests
 * on a real test server.
 */
@DisplayName("FoliaScheduler Tests")
@Disabled("Requires real Folia server - MockBukkit doesn't support regionalized scheduling")
class FoliaSchedulerTest {

    @Test
    void testRunTask() {
        // TODO: Re-enable when testing on real Folia server
    }

    @Test
    void testRunTaskLater() {
        // TODO: Re-enable when testing on real Folia server
    }

    @Test
    void testRunTaskAsynchronously() {
        // TODO: Re-enable when testing on real Folia server
    }
}