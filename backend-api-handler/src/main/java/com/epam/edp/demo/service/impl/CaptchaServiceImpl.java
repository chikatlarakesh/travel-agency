package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.captcha.CaptchaEntry;
import com.epam.edp.demo.config.CaptchaConfig;
import com.epam.edp.demo.dto.auth.CaptchaResponseDTO;
import com.epam.edp.demo.exception.CaptchaValidationException;
import com.epam.edp.demo.service.CaptchaService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Custom CAPTCHA service that generates distorted image challenges without
 * any external API dependency.
 *
 * <p>Design highlights:
 * <ul>
 *   <li>{@link SecureRandom} for cryptographically strong character selection.</li>
 *   <li>Caffeine cache handles TTL expiry and memory-leak prevention automatically.</li>
 *   <li>Each CAPTCHA is single-use: invalidated immediately on the first
 *       validation attempt (successful or failed) after the answer check.</li>
 *   <li>Image noise (dots + lines) and per-character rotation resist OCR bots.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    /**
     * Alphabet excludes visually ambiguous characters: 0/O/o, 1/I/l/i.
     * Improves human readability while keeping entropy high.
     */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CaptchaConfig captchaConfig;
    private final Cache<String, CaptchaEntry> captchaCache;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @Override
    public CaptchaResponseDTO generate() {
        String captchaId = UUID.randomUUID().toString();
        String answer    = buildRandomText(captchaConfig.getLength());
        Instant expiry   = Instant.now().plusSeconds(captchaConfig.getExpirySeconds());

        captchaCache.put(captchaId, new CaptchaEntry(answer, expiry));
        String imageBase64 = renderToBase64(answer);

        log.debug("captcha.generated captchaId={}", captchaId);
        return new CaptchaResponseDTO(captchaId, imageBase64, captchaConfig.getExpirySeconds());
    }

    @Override
    public void validate(String captchaId, String captchaAnswer) {
        // --- Input guard checks (missing token / missing answer) ---
        if (captchaId == null || captchaId.isBlank()) {
            log.warn("captcha.validation.failed reason=MISSING_ID");
            throw new CaptchaValidationException("CAPTCHA ID is required");
        }
        if (captchaAnswer == null || captchaAnswer.isBlank()) {
            log.warn("captcha.validation.failed reason=MISSING_ANSWER captchaId={}", captchaId);
            throw new CaptchaValidationException("CAPTCHA answer is required");
        }

        // --- Retrieve and immediately invalidate (single-use enforcement) ---
        CaptchaEntry entry = captchaCache.getIfPresent(captchaId);
        captchaCache.invalidate(captchaId);

        // Caffeine returns null for both unknown IDs and naturally-expired entries.
        if (entry == null) {
            log.warn("captcha.validation.failed reason=NOT_FOUND_OR_EXPIRED captchaId={}", captchaId);
            throw new CaptchaValidationException("CAPTCHA not found or has expired. Please generate a new one.");
        }

        // Defence-in-depth: double-check wall-clock expiry in case Caffeine lazy-eviction
        // has not yet removed the entry.
        if (Instant.now().isAfter(entry.getExpiresAt())) {
            log.warn("captcha.validation.failed reason=EXPIRED captchaId={}", captchaId);
            throw new CaptchaValidationException("CAPTCHA has expired. Please generate a new one.");
        }

        // --- Answer comparison ---
        boolean correct = captchaConfig.isCaseSensitive()
                ? entry.getAnswer().equals(captchaAnswer)
                : entry.getAnswer().equalsIgnoreCase(captchaAnswer);

        if (!correct) {
            log.warn("captcha.validation.failed reason=WRONG_ANSWER captchaId={}", captchaId);
            throw new CaptchaValidationException("Incorrect CAPTCHA. Please try again.");
        }

        log.debug("captcha.validation.success captchaId={}", captchaId);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private String buildRandomText(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Renders the CAPTCHA text as a distorted PNG image and returns it as a
     * {@code data:image/png;base64,...} data URL ready for use in an img tag.
     */
    private String renderToBase64(String text) {
        int w = captchaConfig.getImageWidth();
        int h = captchaConfig.getImageHeight();

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            configureRenderingHints(g);
            drawBackground(g, w, h);
            drawNoiseLines(g, w, h);
            drawNoiseDots(g, w, h);
            drawCharacters(g, text, w, h);
            drawForegroundLines(g, w, h);
        } finally {
            g.dispose();
        }

        return encodeImage(image);
    }

    private void configureRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
    }

    private void drawBackground(Graphics2D g, int w, int h) {
        g.setColor(new Color(248, 248, 252));
        g.fillRect(0, 0, w, h);
    }

    /** Low-opacity scattered dots — break up uniform regions used by OCR. */
    private void drawNoiseDots(Graphics2D g, int w, int h) {
        for (int i = 0; i < 180; i++) {
            int shade = 180 + SECURE_RANDOM.nextInt(60);
            int blue = Math.min(255, shade + SECURE_RANDOM.nextInt(20));
            g.setColor(new Color(shade, shade, blue));
            g.fillOval(SECURE_RANDOM.nextInt(w), SECURE_RANDOM.nextInt(h), 2, 2);
        }
    }

    /** Thin random lines across the background confuse edge-detection. */
    private void drawNoiseLines(Graphics2D g, int w, int h) {
        for (int i = 0; i < 6; i++) {
            int shade = 190 + SECURE_RANDOM.nextInt(50);
            g.setColor(new Color(shade, shade, shade));
            g.setStroke(new BasicStroke(0.8f + SECURE_RANDOM.nextFloat() * 0.7f));
            g.drawLine(SECURE_RANDOM.nextInt(w), SECURE_RANDOM.nextInt(h),
                       SECURE_RANDOM.nextInt(w), SECURE_RANDOM.nextInt(h));
        }
    }

    /**
     * Draws each character at a random vertical offset with a slight random
     * rotation to defeat simple OCR segmentation.
     */
    private void drawCharacters(Graphics2D g, String text, int w, int h) {
        int len = text.length();
        int slotW = (w - 20) / len;

        for (int i = 0; i < len; i++) {
            AffineTransform saved = g.getTransform();

            int x = 10 + i * slotW + SECURE_RANDOM.nextInt(Math.max(1, slotW / 4));
            int y = h / 2 + 9 + SECURE_RANDOM.nextInt(8) - 4;

            // Rotation: ±~20 degrees
            double angle = (SECURE_RANDOM.nextDouble() - 0.5) * 0.7;
            g.rotate(angle, x + slotW / 2.0, (double) h / 2);

            // Dark random colour per character
            g.setColor(new Color(
                    SECURE_RANDOM.nextInt(80),
                    SECURE_RANDOM.nextInt(80),
                    SECURE_RANDOM.nextInt(80)));

            // Slight font-size variation per character
            g.setFont(new Font("Arial", Font.BOLD, 24 + SECURE_RANDOM.nextInt(8)));
            g.drawString(String.valueOf(text.charAt(i)), x, y);

            g.setTransform(saved);
        }
    }

    /** Semi-transparent overlay lines on top of the text hinder neural segmenters. */
    private void drawForegroundLines(Graphics2D g, int w, int h) {
        for (int i = 0; i < 3; i++) {
            g.setColor(new Color(
                    100 + SECURE_RANDOM.nextInt(100),
                    100 + SECURE_RANDOM.nextInt(100),
                    100 + SECURE_RANDOM.nextInt(100),
                    130));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(0, 10 + SECURE_RANDOM.nextInt(h - 20),
                       w, 10 + SECURE_RANDOM.nextInt(h - 20));
        }
    }

    private String encodeImage(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            log.error("captcha.image.encode.failed", e);
            throw new IllegalStateException("Failed to encode CAPTCHA image", e);
        }
    }
}
