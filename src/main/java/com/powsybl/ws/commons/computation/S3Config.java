package com.powsybl.ws.commons.computation;

import com.powsybl.ws.commons.computation.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Config {
    @Value("${spring.cloud.aws.bucket}")
    private String bucketName;

    @Bean
    public S3Service s3Service(S3Client s3Client) {
        return new S3Service(s3Client, bucketName);
    }
}
