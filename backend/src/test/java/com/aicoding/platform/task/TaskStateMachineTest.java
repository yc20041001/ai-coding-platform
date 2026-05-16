package com.aicoding.platform.task;

import com.aicoding.platform.task.application.TaskApplicationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the VALID_TRANSITIONS state machine from TaskApplicationService.
 * Uses a test-only subclass to expose the package-private validateTransition method.
 */
class TaskStateMachineTest {

    private final TestTaskService service = new TestTaskService();

    // === Valid transitions ===

    @Test
    void shouldAllowPendingToRunning() {
        service.validateTransition("PENDING", "RUNNING");
    }

    @Test
    void shouldAllowPendingToCanceled() {
        service.validateTransition("PENDING", "CANCELED");
    }

    @Test
    void shouldAllowRunningToCompleted() {
        service.validateTransition("RUNNING", "COMPLETED");
    }

    @Test
    void shouldAllowRunningToFailed() {
        service.validateTransition("RUNNING", "FAILED");
    }

    @Test
    void shouldAllowRunningToCanceled() {
        service.validateTransition("RUNNING", "CANCELED");
    }

    @Test
    void shouldAllowRunningToReviewing() {
        service.validateTransition("RUNNING", "REVIEWING");
    }

    @Test
    void shouldAllowFailedToPending() {
        service.validateTransition("FAILED", "PENDING");
    }

    @Test
    void shouldAllowReviewingToCanceled() {
        service.validateTransition("REVIEWING", "CANCELED");
    }

    // === Invalid transitions ===

    @Test
    void shouldRejectCanceledToRunning() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("CANCELED", "RUNNING"));
    }

    @Test
    void shouldRejectCompletedToRunning() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("COMPLETED", "RUNNING"));
    }

    @Test
    void shouldRejectCompletedToFailed() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("COMPLETED", "FAILED"));
    }

    @Test
    void shouldRejectFailedToRunning() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("FAILED", "RUNNING"));
    }

    @Test
    void shouldRejectFailedToCompleted() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("FAILED", "COMPLETED"));
    }

    @Test
    void shouldRejectReviewingToRunning() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("REVIEWING", "RUNNING"));
    }

    @Test
    void shouldRejectPendingToCompleted() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("PENDING", "COMPLETED"));
    }

    @Test
    void shouldRejectCanceledToPending() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("CANCELED", "PENDING"));
    }

    @Test
    void shouldRejectNonexistentStatus() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("NONEXISTENT", "RUNNING"));
    }

    // === Edge cases ===

    @Test
    void shouldRejectTransitionToSameStatus() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("PENDING", "PENDING"));
    }

    @Test
    void shouldRejectTransitionFromNull() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition(null, "RUNNING"));
    }

    @Test
    void shouldRejectTransitionToNull() {
        assertThrows(RuntimeException.class, () ->
                service.validateTransition("PENDING", null));
    }

    /**
     * Test-only subclass that exposes the protected validateTransition method.
     */
    static class TestTaskService extends TaskApplicationService {
        TestTaskService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public void validateTransition(String currentStatus, String newStatus) {
            super.validateTransition(currentStatus, newStatus);
        }
    }
}
