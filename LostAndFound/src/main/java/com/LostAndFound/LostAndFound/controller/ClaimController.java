package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.dto.ClaimDTO;
import com.LostAndFound.LostAndFound.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimDTO> createClaim(@RequestBody ClaimDTO claimDTO) {
        return ResponseEntity.ok(claimService.createClaim(claimDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimDTO> getClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    @GetMapping
    public ResponseEntity<List<ClaimDTO>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClaimDTO> updateClaim(@PathVariable Long id, @RequestBody ClaimDTO claimDTO) {
        return ResponseEntity.ok(claimService.updateClaim(id, claimDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }
}
