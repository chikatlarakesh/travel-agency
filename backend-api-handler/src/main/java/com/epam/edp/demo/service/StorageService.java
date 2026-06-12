package com.epam.edp.demo.service;

/**
 * Abstraction over object storage (AWS S3).
 * Decouples service logic from the concrete storage provider,
 * making local/test stubs straightforward to plug in.
 */
public interface StorageService {

    /**
     * Upload raw bytes to the given key under the configured bucket.
     *
     * @param key         the S3 object key (e.g. "avatars/{userId}/profile.jpg")
     * @param data        the file bytes
     * @param contentType MIME type (e.g. "image/jpeg")
     * @return the public HTTPS URL of the uploaded object
     */
    String upload(String key, byte[] data, String contentType);
}
