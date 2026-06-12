package com.epam.edp.demo.config;

import com.epam.edp.demo.captcha.CaptchaEntry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Data
@Configuration
@ConfigurationProperties(prefix = "captcha")
public class CaptchaConfig {

    /** Number of characters in the generated CAPTCHA text. */
    private int length = 6;

    /** Seconds before a CAPTCHA token expires. */
    private int expirySeconds = 300;

    /** Width of the generated CAPTCHA image in pixels. */
    private int imageWidth = 200;

    /** Height of the generated CAPTCHA image in pixels. */
    private int imageHeight = 60;

    /** Maximum number of concurrent pending CAPTCHAs in memory. */
    private int maxCacheSize = 10000;

    /** When false (default), answer comparison is case-insensitive. */
    private boolean caseSensitive = false;

    /**
     * Thread-safe Caffeine cache that automatically expires entries after
     * {@code expirySeconds} seconds, preventing memory leaks from
     * abandoned CAPTCHA challenges.
     */
    @Bean
    public Cache<String, CaptchaEntry> captchaCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expirySeconds, TimeUnit.SECONDS)
                .maximumSize(maxCacheSize)
                .build();
    }
}
