package com.example.feature.proofs.service;

import com.example.feature.proofs.dto.ProofResponse;
import com.example.feature.proofs.dto.ProofSubmissionRequest;

public interface ProofService {
    ProofResponse submitProof(ProofSubmissionRequest request);
}
