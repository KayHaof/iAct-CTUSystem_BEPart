package com.example.activityservice.feature.proofs.service;


import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;

public interface ProofService {
    ProofResponse submitProof(ProofSubmissionRequest request);
    ProofResponse approveProof(Long proofId);
    ProofResponse rejectProof(Long proofId, String reason);
}
