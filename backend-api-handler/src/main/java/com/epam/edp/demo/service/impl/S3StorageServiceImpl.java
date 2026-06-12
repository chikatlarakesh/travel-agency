package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    public S3StorageServiceImpl(
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${aws.region:us-east-1}") String region) {
        this.bucketName = bucketName;
        this.region = region;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public String upload(String key, byte[] data, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));

        String url = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
        log.debug("Uploaded to S3: {}", url);
        return url;
    }
}
