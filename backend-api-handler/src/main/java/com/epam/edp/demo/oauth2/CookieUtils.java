package com.epam.edp.demo.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Utility helpers for reading and writing HTTP cookies.
 *
 * <p>Used exclusively by {@link CookieOAuth2AuthorizationRequestRepository} to persist
 * the OAuth2 authorization-request state across the provider redirect round-trip without
 * requiring an HTTP session — keeping the application fully stateless and safe for
 * horizontally-scaled Kubernetes deployments.
 *
 * <p>All methods are static and the class is non-instantiable.
 */
public final class CookieUtils {

    private CookieUtils() {}

    /** Returns the first cookie matching {@code name}, if present in the request. */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .findFirst();
    }

    /**
     * Adds a short-lived, HttpOnly, path-scoped cookie to the response.
     *
     * @param maxAgeSeconds positive value sets TTL; 0 expires the cookie immediately
     */
    public static void addCookie(HttpServletResponse response,
                                  String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    /** Expires (deletes) the named cookie if it exists on the current request. */
    public static void deleteCookie(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String name) {
        getCookie(request, name).ifPresent(c -> {
            Cookie expired = new Cookie(name, "");
            expired.setPath("/");
            expired.setMaxAge(0);
            response.addCookie(expired);
        });
    }

    /**
     * Serializes a {@link java.io.Serializable} object to a URL-safe Base64 string
     * using standard Java object serialization.
     *
     * <p>Spring Security's {@code OAuth2AuthorizationRequest} implements
     * {@code Serializable}, so this round-trips safely.
     */
    public static String serialize(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize object for cookie storage", e);
        }
    }

    /** Deserializes an object previously serialized via {@link #serialize(Object)}. */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize object from cookie", e);
        }
    }
}

