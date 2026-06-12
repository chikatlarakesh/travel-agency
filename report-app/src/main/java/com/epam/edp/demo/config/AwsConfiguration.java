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

    @Bean
    public StaticCredentialsProvider userCredentialsProvider() {
        String key    = normalize(accessKeyId);
        String secret = normalize(secretAccessKey);
        String token  = normalize(sessionToken);

        if (key.isEmpty() || secret.isEmpty() || token.isEmpty()) {
            throw new IllegalStateException(
                "Missing AWS temporary credentials. Set aws.access-key-id, aws.secret-access-key, aws.session-token.");
        }

        log.info("aws.credentials.loaded region={}", normalize(region));
        return StaticCredentialsProvider.create(
                AwsSessionCredentials.create(key, secret, token));
    }

    @Bean
    public StsAssumeRoleCredentialsProvider assumeRoleCredentialsProvider() {
        String arn = normalize(roleArn);
        if (arn.isEmpty()) {
            throw new IllegalStateException("Missing AWS role ARN. Set aws.role-arn.");
        }

        StsClient stsClient = StsClient.builder()
                .region(Region.of(normalize(region)))
                .credentialsProvider(userCredentialsProvider())
                .build();

        log.info("aws.sts.assume-role role={}", arn);
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(b -> b
                        .roleArn(arn)
                        .roleSessionName("report-app-ses-session"))
                .build();
    }

    @Bean
    public SesClient sesClient() {
        log.info("aws.ses.client region={}", normalize(region));
        return SesClient.builder()
                .credentialsProvider(assumeRoleCredentialsProvider())
                .region(Region.of(normalize(region)))
                .build();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
