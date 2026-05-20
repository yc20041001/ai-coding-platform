package com.aicoding.platform.task;

import com.aicoding.platform.common.exception.BizException;
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
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("CANCELED", "RUNNING"));
        assertEquals("不允许的状态流转: CANCELED -> RUNNING", ex.getMessage());
    }

    @Test
    void shouldRejectCompletedToRunning() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("COMPLETED", "RUNNING"));
        assertEquals("不允许的状态流转: COMPLETED -> RUNNING", ex.getMessage());
    }

    @Test
    void shouldRejectCompletedToFailed() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("COMPLETED", "FAILED"));
        assertEquals("不允许的状态流转: COMPLETED -> FAILED", ex.getMessage());
    }

    @Test
    void shouldRejectFailedToRunning() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("FAILED", "RUNNING"));
        assertEquals("不允许的状态流转: FAILED -> RUNNING", ex.getMessage());
    }

    @Test
    void shouldRejectFailedToCompleted() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("FAILED", "COMPLETED"));
        assertEquals("不允许的状态流转: FAILED -> COMPLETED", ex.getMessage());
    }

    @Test
    void shouldRejectReviewingToRunning() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("REVIEWING", "RUNNING"));
        assertEquals("不允许的状态流转: REVIEWING -> RUNNING", ex.getMessage());
    }

    @Test
    void shouldRejectPendingToCompleted() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("PENDING", "COMPLETED"));
        assertEquals("不允许的状态流转: PENDING -> COMPLETED", ex.getMessage());
    }

    @Test
    void shouldRejectCanceledToPending() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("CANCELED", "PENDING"));
        assertEquals("不允许的状态流转: CANCELED -> PENDING", ex.getMessage());
    }

    @Test
    void shouldRejectNonexistentStatus() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("NONEXISTENT", "RUNNING"));
        assertEquals("不允许的状态流转: NONEXISTENT -> RUNNING", ex.getMessage());
    }

    // === Edge cases ===

    @Test
    void shouldRejectTransitionToSameStatus() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("PENDING", "PENDING"));
        assertEquals("不允许的状态流转: PENDING -> PENDING", ex.getMessage());
    }

    @Test
    void shouldRejectTransitionFromNull() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition(null, "RUNNING"));
        assertEquals("不允许的状态流转: null -> RUNNING", ex.getMessage());
    }

    @Test
    void shouldRejectTransitionToNull() {
        BizException ex = assertThrows(BizException.class, () ->
                service.validateTransition("PENDING", null));
        assertEquals("不允许的状态流转: PENDING -> null", ex.getMessage());
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
