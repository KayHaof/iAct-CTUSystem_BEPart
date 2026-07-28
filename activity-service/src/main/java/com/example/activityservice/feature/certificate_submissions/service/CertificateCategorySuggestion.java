package com.example.activityservice.feature.certificate_submissions.service;

import com.example.activityservice.feature.categories.model.Categories;

public record CertificateCategorySuggestion(
        Categories category,
        String categoryName,
        Integer point,
        String reason
) {
    public static CertificateCategorySuggestion empty(String categoryName, String reason) {
        return new CertificateCategorySuggestion(null, categoryName, null, reason);
    }
}
