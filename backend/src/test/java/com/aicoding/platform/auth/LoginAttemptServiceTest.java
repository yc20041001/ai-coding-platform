package com.aicoding.platform.auth;

import com.aicoding.platform.auth.application.LoginAttemptService;
import com.aicoding.platform.auth.config.LoginProtectionProperties;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private static LoginAttemptService newService() {
        LoginProtectionProperties props = new LoginProtectionProperties();
        props.setEnabled(true);
        props.setMaxEmailFailures(3);
        props.setMaxIpFailures(5);
        props.setFailureWindowSeconds(300);
        props.setLockSeconds(60);
        return new LoginAttemptService(props, Optional.empty());
    }

    @Test
    void shouldAllowLoginWhenNotLocked() {
        LoginAttemptService service = newService();
        service.checkLocked("test@example.com", "192.168.1.1");
    }

    @Test
    void shouldRecordAndAccumulateEmailFailures() {
        LoginAttemptService service = newService();
        String email = "failtest@example.com";
        String ip = "10.0.0.1";

        for (int i = 0; i < 2; i++) {
            service.recordFailure(email, ip);
        }

        service.checkLocked(email, ip);
    }

    @Test
    void shouldLockAfterMaxEmailFailures() {
        LoginAttemptService service = newService();
        String email = "lockme@example.com";
        String ip = "10.0.0.2";

        for (int i = 0; i < 3; i++) {
            service.recordFailure(email, ip);
        }

        assertThatThrownBy(() -> service.checkLocked(email, ip))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_TOO_MANY_ATTEMPTS));
    }

    @Test
    void shouldLockAfterMaxIpFailures() {
        LoginAttemptService service = newService();
        String email = "test@example.com";
        String ip = "10.0.0.99";

        for (int i = 0; i < 5; i++) {
            service.recordFailure(email, ip);
        }

        assertThatThrownBy(() -> service.checkLocked(email, ip))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_TOO_MANY_ATTEMPTS));
    }

    @Test
    void shouldClearFailuresOnSuccess() {
        LoginAttemptService service = newService();
        String email = "cleartest@example.com";
        String ip = "10.0.0.50";

        for (int i = 0; i < 2; i++) {
            service.recordFailure(email, ip);
        }

        service.recordSuccess(email, ip);

        service.checkLocked(email, ip);
    }

    @Test
    void shouldNotAffectOtherEmailsWhenOneIsLocked() {
        LoginAttemptService service = newService();
        String lockedEmail = "locked@example.com";
        String differentEmail = "other@example.com";
        String ip = "10.0.0.100";

        for (int i = 0; i < 3; i++) {
            service.recordFailure(lockedEmail, ip);
        }

        service.checkLocked(differentEmail, ip);
    }

    @Test
    void shouldBeDisabledByDefault() {
        LoginProtectionProperties disabledProps = new LoginProtectionProperties();
        disabledProps.setEnabled(false);
        LoginAttemptService disabledService = new LoginAttemptService(disabledProps, Optional.empty());

        disabledService.checkLocked("any@example.com", "any-ip");
        disabledService.recordFailure("any@example.com", "any-ip");
        disabledService.recordSuccess("any@example.com", "any-ip");
    }

    @Test
    void shouldNormalizeEmailToLowercase() {
        LoginAttemptService service = newService();
        String email1 = "Test@Example.COM";
        String email2 = "test@example.com";
        String ip = "10.0.0.200";

        for (int i = 0; i < 3; i++) {
            service.recordFailure(email1, ip);
        }

        assertThatThrownBy(() -> service.checkLocked(email2, ip))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_TOO_MANY_ATTEMPTS));
    }

    @Test
    void shouldHandleNullEmailGracefully() {
        LoginAttemptService service = newService();
        service.recordFailure(null, "127.0.0.1");
        service.checkLocked(null, "127.0.0.1");
    }

    @Test
    void shouldReturnUnknownIpWhenNoRequest() {
        LoginAttemptService service = newService();
        String ip = service.currentClientIp();
        assertThat(ip).isEqualTo("unknown");
    }
}
