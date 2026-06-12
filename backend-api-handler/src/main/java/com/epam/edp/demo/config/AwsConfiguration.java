package com.epam.edp.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

/**
 * AWS Configuration for SES email delivery via STS AssumeRole pattern.
 * 
 * This configuration:
 * 1. Reads temporary user credentials (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)
 * 2. Assumes the Developer role (AWS_ROLE_ARN) to obtain elevated credentials
 * 3. Creates SES client with assumed role credentials for email delivery
 * 
 * Follows EPAM Project Education pattern for cross-account AWS access.
 */
@Slf4j
@Configuration
public class AwsConfiguration {

        @Value("${aws.access-key-id:}")
        private String accessKeyId;

        @Value("${aws.secret-access-key:}")
        private String secretAccessKey;

        @Value("${aws.session-token:}")
        private String sessionToken;

        @Value("${aws.region:us-east-1}")
        private String region;

        @Value("${aws.role-arn:}")
        private String roleArn;

    /**
     * Provides temporary user credentials (12-hour expiration).
     * These credentials can only be used to assume the Developer role.
     */
    @Bean
    public StaticCredentialsProvider userCredentialsProvider() {
        String accessKey = normalize(accessKeyId);
        String secretKey = normalize(secretAccessKey);
        String token = normalize(sessionToken);

        if (accessKey.isEmpty() || secretKey.isEmpty() || token.isEmpty()) {
            throw new IllegalStateException("Missing AWS temporary user credentials. Set aws.access-key-id, aws.secret-access-key, aws.session-token.");
        }

        log.info("Creating user credentials provider for region: {}", normalize(region));
        return StaticCredentialsProvider.create(
                AwsSessionCredentials.create(
                        accessKey,
                        secretKey,
                        token
                )
        );
    }

    /**
     * Provides Developer role credentials obtained via STS AssumeRole.
     * These credentials are automatically refreshed as needed.
     * The Developer role credentials can be used to access AWS resources.
     */
    @Bean
    public StsAssumeRoleCredentialsProvider assumeRoleCredentialsProvider() {
        String awsRoleArn = normalize(roleArn);
        if (awsRoleArn.isEmpty()) {
            throw new IllegalStateException("Missing AWS role ARN. Set aws.role-arn.");
        }

        String awsRegion = normalize(region);
        log.info("Creating STS assume-role credentials provider for role: {}", awsRoleArn);
        StsClient stsClient = StsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(userCredentialsProvider())
                .build();
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(builder -> builder
                        .roleArn(awsRoleArn)
                        .roleSessionName("travel-agency-ses-session")
                )
                .build();
    }

    /**
     * Provides SES client configured with assumed role credentials.
     * Used for sending emails via AWS SES API (not SMTP).
     */
    @Bean
    public SesClient sesClient() {
                String awsRegion = normalize(region);
                log.info("Creating SES client with assumed role credentials for region: {}", awsRegion);
        return SesClient.builder()
                .credentialsProvider(assumeRoleCredentialsProvider())
                                .region(Region.of(awsRegion))
                .build();
    }

        private String normalize(String value) {
                return value == null ? "" : value.trim();
        }
}
