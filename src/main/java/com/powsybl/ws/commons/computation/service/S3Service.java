/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ws.commons.computation.service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class S3Service {

    private final S3Client s3Client;

    private final String bucketName;

    public S3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String uploadFile(File debugFile, int expireAfterHours) throws IOException {
        try {
            String fileName = debugFile.getName();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .metadata(Map.of("original-filename", fileName))
                    .tagging("expire-after-minutes=" + expireAfterHours)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromFile(debugFile));

            return fileName;
        } catch (S3Exception e) {
            throw new IOException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage());
        }
    }

    public String getS3FileName(String s3Key) throws IOException {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            HeadObjectResponse headResponse = s3Client.headObject(headRequest);

            String fileName = headResponse.metadata().get("original-filename");
            if (fileName == null) {
                fileName = s3Key;
            }
            return fileName;
        } catch (S3Exception e) {
            throw new IOException("Failed to get original file name from S3: " + e.awsErrorDetails().errorMessage());
        }

    }

    public InputStream downloadFile(String s3Key) throws IOException {
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
