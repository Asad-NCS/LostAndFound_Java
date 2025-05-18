package com.LostAndFound.LostAndFound.controller;

import com.LostAndFound.LostAndFound.dto.ClaimActionRequestDTO; // Assuming you created this DTO
import com.LostAndFound.LostAndFound.dto.ClaimDTO;
import com.LostAndFound.LostAndFound.service.ClaimService;
import com.LostAndFound.LostAndFound.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    // ... (createClaim, approveClaim, rejectClaim, getClaim, getAllClaims, getClaimsByItem, getClaimsByUser methods remain the same as in artifact claim_controller_updated_v1_with_forward) ...

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createClaim(
            @RequestPart("claimData") String claimDataJson,
            @RequestPart(value = "proofImage", required = false) MultipartFile proofImage
    ) {
        try {
            log.info("Received create claim request. Claim data JSON: {}", claimDataJson);
            ClaimDTO claimDTO = objectMapper.readValue(claimDataJson, ClaimDTO.class);

            if (claimDTO.getUserId() == null) {
                log.error("Claimer User ID is null in DTO for createClaim.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Claimer User ID must be provided. Ensure you are logged in."));
            }

            if (proofImage != null && !proofImage.isEmpty()) {
                String proofImagePath = fileStorageService.store(proofImage);
                claimDTO.setProofImagePath(proofImagePath);
                log.info("Stored proof image for claim, path: {}", proofImagePath);
            }

            ClaimDTO createdClaim = claimService.createClaim(claimDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdClaim);
        } catch (Exception e) {
            log.error("Error creating claim: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Failed to create claim: " + e.getMessage()));
        }
    }

    @PutMapping("/{claimId}/approve")
    public ResponseEntity<?> approveClaim(
            @PathVariable Long claimId,
            @RequestBody ClaimActionRequestDTO actionRequest
    ) {
        try {
            if (actionRequest.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID is required to approve a claim."));
            }
            Long approverUserId = actionRequest.getUserId();
            log.info("User ID {} attempting to approve claim ID: {}", approverUserId, claimId);

            ClaimDTO approvedClaim = claimService.approveClaim(claimId, approverUserId);
            return ResponseEntity.ok(approvedClaim);
        } catch (SecurityException e){
            log.warn("Authorization failed for approving claim ID {}: {}", claimId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        catch (IllegalStateException e) {
            log.warn("Illegal state for approving claim ID {}: {}", claimId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
        catch (RuntimeException e) {
            log.error("Error approving claim ID {}: {}", claimId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{claimId}/reject")
    public ResponseEntity<?> rejectClaim(
            @PathVariable Long claimId,
            @RequestBody ClaimActionRequestDTO actionRequest
    ) {
        try {
            if (actionRequest.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID is required to reject a claim."));
            }
            Long rejecterUserId = actionRequest.getUserId();
            log.info("User ID {} attempting to reject claim ID: {}", rejecterUserId, claimId);

            String reason = actionRequest.getRejectionReason() != null ? actionRequest.getRejectionReason() : "No reason provided.";
            ClaimDTO rejectedClaim = claimService.rejectClaim(claimId, rejecterUserId, reason);
            return ResponseEntity.ok(rejectedClaim);
        } catch (SecurityException e){
            log.warn("Authorization failed for rejecting claim ID {}: {}", claimId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        catch (IllegalStateException e) {
            log.warn("Illegal state for rejecting claim ID {}: {}", claimId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
        catch (RuntimeException e) {
            log.error("Error rejecting claim ID {}: {}", claimId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimDTO> getClaim(@PathVariable Long id) { /* ... same ... */
        try {
            ClaimDTO claim = claimService.getClaimById(id);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e){
            log.warn("Claim not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ClaimDTO>> getAllClaims() { /* ... same ... */
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<ClaimDTO>> getClaimsByItem(@PathVariable Long itemId) { /* ... same ... */
        return ResponseEntity.ok(claimService.getClaimsByItemId(itemId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClaimDTO>> getClaimsByUser(@PathVariable Long userId) { /* ... same ... */
        return ResponseEntity.ok(claimService.getClaimsByUserId(userId));
    }

    // UPDATED: forwardClaimToAdmin endpoint
    @PutMapping("/{claimId}/forward-to-admin")
    public ResponseEntity<?> forwardClaimToAdmin(
            @PathVariable Long claimId,
            @RequestBody ClaimActionRequestDTO actionRequest // Expecting a body with userId
    ) {
        try {
            if (actionRequest.getUserId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID (finderId) is required to forward a claim."));
            }
            Long finderUserId = actionRequest.getUserId(); // Get finder's ID from request body
            log.info("Finder (User ID {}) attempting to forward claim ID: {}", finderUserId, claimId);

            ClaimDTO forwardedClaim = claimService.forwardClaimToAdmin(claimId, finderUserId);
            return ResponseEntity.ok(forwardedClaim);
        } catch (SecurityException e){
            log.warn("Authorization failed for forwarding claim ID {}: {}", claimId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Illegal state for forwarding claim ID {}: {}", claimId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) { // Catch "not found" or other issues
            log.error("Error forwarding claim ID {}: {}", claimId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }


    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> updateClaim(
            @PathVariable("id") Long claimId,
            @RequestPart(value = "claimData", required = true) String claimDataJson,
            @RequestPart(value = "newProofImage", required = false) MultipartFile newProofImage
    ) {
        try {
            ClaimDTO claimDTO = objectMapper.readValue(claimDataJson, ClaimDTO.class);
            Long currentUserId = claimDTO.getUserId(); // Assuming DTO contains the ID of user making the update
            // This needs to be the claim owner.
            if (currentUserId == null) {
                log.error("User ID is null in DTO for updateClaim.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "User ID must be provided for claim update."));
            }
            log.info("User ID {} attempting to update claim ID: {}", currentUserId, claimId);

            if (newProofImage != null && !newProofImage.isEmpty()) {
                ClaimDTO existingClaimDetails = claimService.getClaimById(claimId);
                if (existingClaimDetails.getProofImagePath() != null && !existingClaimDetails.getProofImagePath().isBlank()) {
                    try {
                        fileStorageService.delete(existingClaimDetails.getProofImagePath());
                        log.info("Deleted old proof image {} for claim update ID: {}", existingClaimDetails.getProofImagePath(), claimId);
                    } catch (Exception e) {
                        log.warn("Could not delete old proof image {} for claim ID: {}. Error: {}", existingClaimDetails.getProofImagePath(), claimId, e.getMessage());
                    }
                }
                String newProofImagePath = fileStorageService.store(newProofImage);
                claimDTO.setProofImagePath(newProofImagePath);
                log.info("Stored new proof image for claim update ID: {}, path: {}", claimId, newProofImagePath);
            }
            ClaimDTO updatedClaim = claimService.updateClaim(claimId, claimDTO, currentUserId);
            return ResponseEntity.ok(updatedClaim);
        } catch (SecurityException e) { /* ... */ return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));}
        catch (IllegalStateException e) { /* ... */ return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));}
        catch (Exception e) { /* ... */ return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Failed to update claim: " + e.getMessage()));}
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClaim(@PathVariable Long id) { // Removed unused DTO
        try {
            // TODO: Replace MOCK_DELETER_USER_ID and MOCK_USER_ROLE with actual authenticated user context
            Long MOCK_DELETER_USER_ID = 1L;
            String MOCK_USER_ROLE = "user";
            claimService.deleteClaim(id, MOCK_DELETER_USER_ID, MOCK_USER_ROLE);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e){ /* ... */ return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));}
        catch (IllegalStateException e){ /* ... */ return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));}
        catch (RuntimeException e){ /* ... */ return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));}
    }
    @GetMapping("/admin-review")
    public ResponseEntity<List<ClaimDTO>> getClaimsForAdminReview(
            // @AuthenticationPrincipal UserDetails currentUser // TODO: Secure for ADMIN role
    ) {
        // For now, assuming any call to this is by an authorized admin for testing
        // Later, Spring Security will enforce that only ADMIN role can access this.
        // if (currentUser == null || !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
        //    log.warn("Unauthorized attempt to access admin review claims.");
        //    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        // }
        log.info("Admin request: Fetching claims for review (status: FORWARDED_TO_ADMIN).");
        List<ClaimDTO> claims = claimService.getClaimsForAdminReview();
        return ResponseEntity.ok(claims);
    }
}
