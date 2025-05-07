/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.computation.service;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class S3Service {

    public static final String METADATA_ORIGINAL_FILENAME = "original-filename";

    private final S3Client s3Client;

    private final String bucketName;

    public S3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void uploadFile(File debugFile, String s3Key, Integer expireAfterHours) throws IOException {
        try {
            String fileName = debugFile.getName();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .metadata(Map.of(METADATA_ORIGINAL_FILENAME, fileName))
                    .tagging(expireAfterHours != null ? "expire-after-minutes=" + expireAfterHours : null)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromFile(debugFile));
        } catch (S3Exception e) {
            throw new IOException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage());
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String s3Key) throws IOException {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            return s3Client.getObject(getRequest);
        } catch (S3Exception e) {
            throw new IOException("Failed to download file from S3: " + e.awsErrorDetails().errorMessage());
        }
    }
}
