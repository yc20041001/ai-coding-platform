package com.aicoding.platform.auth.application;

import com.aicoding.platform.auth.config.CaptchaProperties;
import com.aicoding.platform.auth.dto.CaptchaResponse;
import com.aicoding.platform.common.exception.BizException;
import com.aicoding.platform.common.exception.ErrorCode;
import com.aicoding.platform.common.redis.RedisKeys;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaptchaProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CaptchaEntry> memoryStore = new ConcurrentHashMap<>();

    public CaptchaService(CaptchaProperties properties,
                         Optional<StringRedisTemplate> redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate.orElse(null);
    }

    public CaptchaResponse generate() {
        String captchaId = generateId();
        String code = generateCode(properties.getLength());
        CaptchaEntry entry = new CaptchaEntry(code, 0);

        StringRedisTemplate template = redisTemplate;
        if ("redis".equals(properties.getStore()) && isRedisAvailable(template)) {
            try {
                String key = RedisKeys.captcha(captchaId);
                String json = objectMapper.writeValueAsString(entry);
                template.opsForValue().set(required(key), required(json),
                        seconds(properties.getExpireSeconds()));
            } catch (JsonProcessingException | RuntimeException e) {
                log.warn("Failed to store captcha in Redis, falling back to memory: {}", e.getMessage());
                memoryStore.put(captchaId, entry);
            }
        } else {
            memoryStore.put(captchaId, entry);
        }

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

        CaptchaEntry entry = loadEntry(captchaId);
        if (entry == null) {
            throw new BizException(ErrorCode.CAPTCHA_EXPIRED, "验证码已过期，请重新获取");
        }

        String expected = entry.code;
        String actual = captchaCode != null ? captchaCode.trim().toUpperCase() : "";

        if (!expected.equals(actual)) {
            entry.attempts++;
            if (entry.attempts >= properties.getMaxAttempts()) {
                deleteEntry(captchaId);
                throw new BizException(ErrorCode.CAPTCHA_INVALID, "验证码尝试次数过多，请重新获取");
            }
            saveEntry(captchaId, entry);
            throw new BizException(ErrorCode.CAPTCHA_INVALID, "验证码错误");
        }

        deleteEntry(captchaId);
    }

    private CaptchaEntry loadEntry(String captchaId) {
        StringRedisTemplate template = redisTemplate;
        if ("redis".equals(properties.getStore()) && isRedisAvailable(template)) {
            try {
                String key = RedisKeys.captcha(captchaId);
                String json = template.opsForValue().get(required(key));
                if (json != null) {
                    return objectMapper.readValue(json, CaptchaEntry.class);
                }
            } catch (JsonProcessingException | RuntimeException e) {
                log.warn("Failed to load captcha from Redis, falling back to memory: {}", e.getMessage());
            }
        }
        return memoryStore.get(captchaId);
    }

    private void saveEntry(String captchaId, CaptchaEntry entry) {
        StringRedisTemplate template = redisTemplate;
        if ("redis".equals(properties.getStore()) && isRedisAvailable(template)) {
            try {
                String key = RedisKeys.captcha(captchaId);
                String json = objectMapper.writeValueAsString(entry);
                Long ttl = template.getExpire(required(key));
                if (ttl != null && ttl > 0) {
                    template.opsForValue().set(required(key), required(json), seconds(ttl));
                }
            } catch (JsonProcessingException | RuntimeException e) {
                log.warn("Failed to save captcha to Redis, updating memory: {}", e.getMessage());
            }
        }
        memoryStore.put(captchaId, entry);
    }

    private void deleteEntry(String captchaId) {
        StringRedisTemplate template = redisTemplate;
        if ("redis".equals(properties.getStore()) && isRedisAvailable(template)) {
            try {
                String key = RedisKeys.captcha(captchaId);
                template.delete(required(key));
            } catch (RuntimeException e) {
                log.warn("Failed to delete captcha from Redis: {}", e.getMessage());
            }
        }
        memoryStore.remove(captchaId);
    }

    private boolean isRedisAvailable(StringRedisTemplate template) {
        if (template == null) {
            return false;
        }
        try {
            RedisConnectionFactory connectionFactory = template.getConnectionFactory();
            if (connectionFactory != null) {
                connectionFactory.getConnection().ping();
                return true;
            }
        } catch (RuntimeException e) {
            log.debug("Redis not available: {}", e.getMessage());
        }
        return false;
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

    private @NonNull String required(String value) {
        return Objects.requireNonNull(value);
    }

    private @NonNull java.time.Duration seconds(long value) {
        return Objects.requireNonNull(java.time.Duration.ofSeconds(value));
    }

    public static class CaptchaEntry {
        public String code;
        public int attempts;

        public CaptchaEntry() {
        }

        public CaptchaEntry(String code, int attempts) {
            this.code = code;
            this.attempts = attempts;
        }
    }
}
