package com.example.activityservice.feature.certificate_submissions.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FetchedCertificateImage {
    private byte[] bytes;
    private String filename;
    private String contentType;
}
