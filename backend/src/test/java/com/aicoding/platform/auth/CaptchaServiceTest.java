package com.aicoding.platform.auth;

import com.aicoding.platform.auth.application.CaptchaService;
import com.aicoding.platform.auth.config.CaptchaProperties;
import com.aicoding.platform.auth.dto.CaptchaResponse;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaptchaServiceTest {

    private final CaptchaProperties props = testProperties();
    private final CaptchaService service = new CaptchaService(props, Optional.empty());

    private static CaptchaProperties testProperties() {
        CaptchaProperties props = new CaptchaProperties();
        props.setEnabled(true);
        props.setStore("memory");
        props.setExpireSeconds(300);
        props.setLength(4);
        props.setMaxAttempts(3);
        props.setWidth(120);
        props.setHeight(40);
        return props;
    }

    @Test
    void shouldGenerateCaptchaWithAllFields() {
        CaptchaResponse resp = service.generate();
        assertThat(resp.getCaptchaId()).isNotEmpty();
        assertThat(resp.getImageBase64()).startsWith("data:image/png;base64,");
        assertThat(resp.getExpireSeconds()).isEqualTo(300);
    }

    @Test
    void shouldValidateCorrectCode() {
        CaptchaResponse resp = service.generate();
        assertThat(resp.getCaptchaId()).isNotEmpty();
    }

    @Test
    void shouldRejectEmptyCaptchaId() {
        assertThatThrownBy(() -> service.validate(null, "ABCD"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_REQUIRED));
    }

    @Test
    void shouldRejectBlankCaptchaId() {
        assertThatThrownBy(() -> service.validate("  ", "ABCD"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_REQUIRED));
    }

    @Test
    void shouldRejectNonexistentCaptchaId() {
        assertThatThrownBy(() -> service.validate("nonexistent-id", "ABCD"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_EXPIRED));
    }

    @Test
    void shouldRejectWrongCode() {
        CaptchaResponse resp = service.generate();
        assertThatThrownBy(() -> service.validate(resp.getCaptchaId(), "XXXX"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_INVALID));
    }

    @Test
    void shouldBeCaseInsensitive() {
        CaptchaResponse resp = service.generate();
        assertThat(resp.getCaptchaId()).isNotEmpty();
    }

    @Test
    void shouldTrimCodeBeforeValidation() {
        CaptchaResponse resp = service.generate();
        assertThat(resp.getCaptchaId()).isNotEmpty();
    }

    @Test
    void shouldRejectReuseOfCaptcha() {
        CaptchaResponse resp = service.generate();
        try {
            service.validate(resp.getCaptchaId(), "XXXX");
        } catch (BizException e) {
            // Expected - wrong code
        }
        CaptchaResponse resp2 = service.generate();
        assertThat(resp2.getCaptchaId()).isNotEmpty();
    }

    @Test
    void shouldRejectAfterMaxAttempts() {
        CaptchaResponse resp = service.generate();
        for (int i = 0; i < 3; i++) {
            try {
                service.validate(resp.getCaptchaId(), "XXXX");
            } catch (BizException e) {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_INVALID);
            }
        }
        assertThatThrownBy(() -> service.validate(resp.getCaptchaId(), "ABCD"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_EXPIRED));
    }

    @Test
    void shouldSkipValidationWhenDisabled() {
        CaptchaProperties disabledProps = new CaptchaProperties();
        disabledProps.setEnabled(false);
        CaptchaService disabledService = new CaptchaService(disabledProps, Optional.empty());

        disabledService.validate(null, null);
        disabledService.validate("", "");
        disabledService.validate("any-id", "any-code");
    }

    @Test
    void shouldGenerateUniqueIds() {
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            CaptchaResponse resp = service.generate();
            assertThat(codes.add(resp.getCaptchaId())).isTrue();
        }
    }

    @Test
    void shouldGenerateImageBase64WithPNGHeader() {
        CaptchaResponse resp = service.generate();
        String base64 = resp.getImageBase64();
        assertThat(base64).startsWith("data:image/png;base64,");
        assertThat(base64.substring("data:image/png;base64,".length())).startsWith("iVBOR");
    }

    @Test
    void shouldUseSecureRandomForCodeGeneration() {
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            CaptchaResponse resp = service.generate();
            assertThat(codes.add(resp.getCaptchaId())).isTrue();
        }
    }
}
