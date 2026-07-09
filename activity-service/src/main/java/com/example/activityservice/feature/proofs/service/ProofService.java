package com.example.activityservice.feature.proofs.service;


import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface ProofService {
    ProofResponse submitProof(ProofSubmissionRequest request);
    PageDTO<ProofResponse> getProofs(Integer status, Long activityId, Pageable pageable);
    ProofResponse approveProof(Long proofId);
    ProofResponse rejectProof(Long proofId, String reason);
}
