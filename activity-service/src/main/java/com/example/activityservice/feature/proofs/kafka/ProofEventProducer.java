package com.example.activityservice.feature.proofs.kafka;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.kafka.KafkaEnvelopePublisher;
import com.example.activityservice.feature.proofs.model.Proofs;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.example.util.UtcDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProofEventProducer extends KafkaEnvelopePublisher {

    public ProofEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper, "activity-service");
    }

    public void publishSubmitted(Proofs proof) {
        publish(KafkaTopics.PROOF_SUBMITTED, KafkaEventTypes.PROOF_SUBMITTED, "proof",
                String.valueOf(proof.getId()), proofPayload(proof));
    }

    public void publishApproved(Proofs proof) {
        publish(KafkaTopics.PROOF_APPROVED, KafkaEventTypes.PROOF_APPROVED, "proof",
                String.valueOf(proof.getId()), proofPayload(proof));
    }

    public void publishRejected(Proofs proof) {
        publish(KafkaTopics.PROOF_REJECTED, KafkaEventTypes.PROOF_REJECTED, "proof",
                String.valueOf(proof.getId()), proofPayload(proof));
    }

    private Map<String, Object> proofPayload(Proofs proof) {
        Activities activity = proof.getActivity();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("proofId", proof.getId());
        payload.put("userId", proof.getStudentId());
        payload.put("activityId", activity != null ? activity.getId() : null);
        payload.put("activityTitle", activity != null ? activity.getTitle() : null);
        payload.put("ownerUserId", activity != null && activity.getCreatedBy() != null ? activity.getCreatedBy().getId() : null);
        payload.put("status", proof.getStatus());
        payload.put("reason", proof.getRejectionReason());
        payload.put("verifiedBy", proof.getVerifiedBy());
        payload.put("verifiedTime", UtcDateTime.format(proof.getVerifiedTime()));
        return payload;
    }
}
