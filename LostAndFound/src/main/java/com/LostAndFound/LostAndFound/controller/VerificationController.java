package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.dto.VerificationDTO;
import com.LostAndFound.LostAndFound.dto.VerificationRequest;
import com.LostAndFound.LostAndFound.dto.VerificationSubmit;
import com.LostAndFound.LostAndFound.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/request")
    public ResponseEntity<VerificationDTO> requestVerification(
            @RequestBody VerificationRequest request) {
        return ResponseEntity.ok(verificationService.requestVerification(
                request.getClaimId(),
                request.getUserId()
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationDTO> verifyClaim(
            @RequestBody VerificationSubmit submit) {
        return ResponseEntity.ok(verificationService.verifyClaim(
                submit.getCode(),
                submit.getClaimId()
        ));
    }

    @GetMapping("/claim/{claimId}")
    public ResponseEntity<VerificationDTO> getVerificationByClaim(
            @PathVariable Long claimId) {
        return ResponseEntity.ok(verificationService.getVerificationByClaimId(claimId));
    }
}