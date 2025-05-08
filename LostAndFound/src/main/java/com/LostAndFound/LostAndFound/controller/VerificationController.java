package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.dto.VerificationDTO;
import com.LostAndFound.LostAndFound.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping
    public ResponseEntity<VerificationDTO> createVerification(@RequestBody VerificationDTO verificationDTO) {
        return ResponseEntity.ok(verificationService.createVerification(verificationDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VerificationDTO> getVerification(@PathVariable Long id) {
        return ResponseEntity.ok(verificationService.getVerificationById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVerification(@PathVariable Long id) {
        verificationService.deleteVerification(id);
        return ResponseEntity.noContent().build();
    }
}
