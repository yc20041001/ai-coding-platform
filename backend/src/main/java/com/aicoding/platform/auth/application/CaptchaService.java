package com.aicoding.platform.auth.application;

import com.aicoding.platform.auth.config.CaptchaProperties;
import com.aicoding.platform.auth.dto.CaptchaResponse;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaptchaProperties properties;
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    public CaptchaService(CaptchaProperties properties) {
        this.properties = properties;
    }

    public CaptchaResponse generate() {
        String captchaId = generateId();
        String code = generateCode(properties.getLength());
        long expireTime = System.currentTimeMillis() + properties.getExpireSeconds() * 1000L;
        store.put(captchaId, new CaptchaEntry(code, expireTime, 0));

        BufferedImage image = generateImage(code);
        String imageBase64 = encodeToBase64(image);

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaId(captchaId);
        response.setImageBase64("data:image/png;base64," + imageBase64);
        response.setExpireSeconds(properties.getExpireSeconds());
        return response;
    }

    public void validate(String captchaId, String captchaCode) {
        if (!properties.isEnabled()) {
            return;
        }

        if (captchaId == null || captchaId.isBlank()) {
            throw new BizException(ErrorCode.CAPTCHA_REQUIRED, "验证码不能为空");
        }

        CaptchaEntry entry = store.get(captchaId);
        if (entry == null) {
            throw new BizException(ErrorCode.CAPTCHA_EXPIRED, "验证码已过期，请重新获取");
        }

        if (System.currentTimeMillis() > entry.expireTime) {
            store.remove(captchaId);
            throw new BizException(ErrorCode.CAPTCHA_EXPIRED, "验证码已过期，请重新获取");
        }

        if (entry.attempts >= properties.getMaxAttempts()) {
            store.remove(captchaId);
            throw new BizException(ErrorCode.CAPTCHA_INVALID, "验证码尝试次数过多，请重新获取");
        }

        String expected = entry.code;
        String actual = captchaCode != null ? captchaCode.trim().toUpperCase() : "";

        if (!expected.equals(actual)) {
            entry.attempts++;
            if (entry.attempts >= properties.getMaxAttempts()) {
                store.remove(captchaId);
                throw new BizException(ErrorCode.CAPTCHA_INVALID, "验证码尝试次数过多，请重新获取");
            }
            store.put(captchaId, entry);
            throw new BizException(ErrorCode.CAPTCHA_INVALID, "验证码错误");
        }

        store.remove(captchaId);
    }

    private String generateId() {
        return Long.toHexString(System.currentTimeMillis()) +
               Long.toHexString(RANDOM.nextLong());
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private BufferedImage generateImage(String code) {
        int w = properties.getWidth();
        int h = properties.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(240, 244, 248));
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(180, 195, 220));
        for (int i = 0; i < 60; i++) {
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            g.fillOval(x, y, 2, 2);
        }

        g.setFont(new Font("Arial", Font.BOLD, 26));
        int charWidth = w / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            String ch = String.valueOf(code.charAt(i));
            Color color = new Color(
                    RANDOM.nextInt(80) + 30,
                    RANDOM.nextInt(80) + 40,
                    RANDOM.nextInt(80) + 120
            );
            g.setColor(color);
            int x = charWidth + i * charWidth;
            int y = h / 2 + RANDOM.nextInt(10) - 5;
            g.drawString(ch, x, y);
        }

        g.dispose();
        return image;
    }

    private String encodeToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode captcha image", e);
        }
    }

    private static class CaptchaEntry {
        final String code;
        final long expireTime;
        int attempts;

        CaptchaEntry(String code, long expireTime, int attempts) {
            this.code = code;
            this.expireTime = expireTime;
            this.attempts = attempts;
        }
    }
}
