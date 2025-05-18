package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.ClaimDTO;
import com.LostAndFound.LostAndFound.model.Claim;
import com.LostAndFound.LostAndFound.model.Item;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.model.ClaimStatus; // Ensure this is imported
import com.LostAndFound.LostAndFound.repository.ClaimRepository;
import com.LostAndFound.LostAndFound.repository.ItemRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
// Import NotificationService if you're ready to integrate basic notification creation
// import com.LostAndFound.LostAndFound.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    // private final NotificationService notificationService; // Uncomment when ready for notifications

    @Transactional
    public ClaimDTO createClaim(ClaimDTO claimDTO) {
        log.info("Attempting to create claim for item ID: {} by user ID: {}", claimDTO.getItemId(), claimDTO.getUserId());

        User claimant = userRepository.findById(claimDTO.getUserId())
                .orElseThrow(() -> {
                    log.error("Claim creation failed: User not found with ID: {}", claimDTO.getUserId());
                    return new RuntimeException("User (claimant) not found with ID: " + claimDTO.getUserId());
                });

        Item item = itemRepository.findById(claimDTO.getItemId())
                .orElseThrow(() -> {
                    log.error("Claim creation failed: Item not found with ID: {}", claimDTO.getItemId());
                    return new RuntimeException("Item not found with ID: " + claimDTO.getItemId());
                });

        // A user cannot claim their own reported item through this flow
        if (item.getUser().getId().equals(claimant.getId())) {
            log.warn("Claim creation blocked: User {} attempting to claim their own item ID: {}", claimant.getId(), item.getId());
            throw new IllegalStateException("You cannot claim an item you reported.");
        }

        // According to the flow: Owner claims a "Found" item.
        // So, the item being claimed should be a "Found" item (isLost = false)
        if (item.isLost()) {
            log.warn("Claim creation blocked: Item ID {} is a 'Lost' item listing, not a 'Found' item listing.", item.getId());
            throw new IllegalStateException("Claims can only be made on items reported as 'Found'.");
        }

        if (item.isClaimed()) {
            log.warn("Claim creation blocked: Item ID {} is already claimed.", item.getId());
            throw new IllegalStateException("This item has already been claimed by someone.");
        }

        // Check if this user already has an active (pending) claim for this item
        boolean existingPendingClaim = claimRepository.existsByUserIdAndItemIdAndStatus(claimant.getId(), item.getId(), ClaimStatus.PENDING);
        if (existingPendingClaim) {
            log.warn("Claim creation blocked: User ID {} already has a pending claim for item ID {}.", claimant.getId(), item.getId());
            throw new IllegalStateException("You already have a pending claim for this item.");
        }


        Claim claim = new Claim();
        claim.setDescription(claimDTO.getDescription());
        claim.setProofImagePath(claimDTO.getProofImagePath()); // Set from DTO
        claim.setStatus(ClaimStatus.PENDING); // Default status for new claims
        claim.setClaimDate(LocalDateTime.now());
        claim.setUser(claimant); // The user making the claim
        claim.setItem(item);     // The item being claimed

        Claim savedClaim = claimRepository.save(claim);
        log.info("Successfully created claim ID: {} for item ID: {} by user ID: {}", savedClaim.getId(), item.getId(), claimant.getId());

        // TODO: Trigger notification to item.getUser() (the finder) about the new claim
        // Example: notificationService.createClaimNotification(item.getUser(), savedClaim);

        return convertToDTO(savedClaim);
    }

    @Transactional
    public ClaimDTO approveClaim(Long claimId, Long approverUserId) { // approverUserId is the ID of the item's reporter (finder)
        log.info("Attempting to approve claim ID: {} by user ID: {}", claimId, approverUserId);
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> {
                    log.error("Claim approval failed: Claim not found with ID: {}", claimId);
                    return new RuntimeException("Claim not found with ID: " + claimId);
                });

        Item item = claim.getItem();
        if (item == null) { // Should not happen due to DB constraints if set up correctly
            log.error("Claim approval failed: Item associated with claim ID {} is null.", claimId);
            throw new IllegalStateException("Claim is not associated with any item.");
        }

        // Security Check: Only the user who reported the item as "Found" can approve its claim.
        if (!item.getUser().getId().equals(approverUserId)) {
            log.warn("Claim approval failed: User ID {} is not authorized to approve claim ID {} for item ID {} (owner is {}).",
                    approverUserId, claimId, item.getId(), item.getUser().getId());
            throw new SecurityException("You are not authorized to approve this claim.");
        }

        if (item.isClaimed()) { // Check on the item itself
            log.warn("Claim approval failed: Item ID {} is already marked as claimed.", item.getId());
            throw new IllegalStateException("This item has already been claimed.");
        }
        if (claim.getStatus() != ClaimStatus.PENDING) {
            log.warn("Claim approval failed: Claim ID {} is not in PENDING status (current status: {}).", claimId, claim.getStatus());
            throw new IllegalStateException("This claim is not pending approval (current status: " + claim.getStatus() + ").");
        }


        claim.setStatus(ClaimStatus.APPROVED);
        // Item updates
        item.setClaimed(true);
        item.setClaimedByUser(claim.getUser()); // User who made the successful claim
        item.setClaimedDate(LocalDateTime.now());
        itemRepository.save(item); // Persist changes to item

        // Optional: Reject other PENDING claims for this item
        List<Claim> otherPendingClaims = claimRepository.findByItemIdAndStatus(item.getId(), ClaimStatus.PENDING);
        for (Claim otherClaim : otherPendingClaims) {
            if (!otherClaim.getId().equals(claimId)) {
                otherClaim.setStatus(ClaimStatus.REJECTED); // Or a new status like SUPERSEDED
                claimRepository.save(otherClaim);
                log.info("Automatically rejected pending claim ID: {} for item ID: {}", otherClaim.getId(), item.getId());
                // TODO: Trigger notification to these other claimants
            }
        }

        Claim updatedClaim = claimRepository.save(claim); // Persist changes to current claim
        log.info("Successfully approved claim ID: {} for item ID: {}. Item now marked as claimed by user ID: {}",
                updatedClaim.getId(), item.getId(), item.getClaimedByUser().getId());

        // TODO: Trigger notification to claim.getUser() (the successful claimant)
        // Example: notificationService.createClaimApprovedNotification(claim.getUser(), updatedClaim);

        return convertToDTO(updatedClaim);
    }

    @Transactional
    public ClaimDTO rejectClaim(Long claimId, Long rejecterUserId, String rejectionReason) { // rejecterUserId is the ID of the item's reporter (finder)
        log.info("Attempting to reject claim ID: {} by user ID: {}", claimId, rejecterUserId);
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> {
                    log.error("Claim rejection failed: Claim not found with ID: {}", claimId);
                    return new RuntimeException("Claim not found with ID: " + claimId);
                });

        Item item = claim.getItem();
        if (item == null) {
            log.error("Claim rejection failed: Item associated with claim ID {} is null.", claimId);
            throw new IllegalStateException("Claim is not associated with any item.");
        }

        // Security Check: Only the user who reported the item can reject its claim.
        if (!item.getUser().getId().equals(rejecterUserId)) {
            log.warn("Claim rejection failed: User ID {} is not authorized to reject claim ID {} for item ID {}.",
                    rejecterUserId, claimId, item.getId());
            throw new SecurityException("You are not authorized to reject this claim.");
        }

        if (claim.getStatus() != ClaimStatus.PENDING) {
            log.warn("Claim rejection failed: Claim ID {} is not in PENDING status (current status: {}).", claimId, claim.getStatus());
            throw new IllegalStateException("This claim is not pending a decision (current status: " + claim.getStatus() + ").");
        }

        claim.setStatus(ClaimStatus.REJECTED);
        // TODO: Potentially store rejectionReason in the Claim entity if you add a field for it.
        // claim.setRejectionReason(rejectionReason);

        Claim updatedClaim = claimRepository.save(claim);
        log.info("Successfully rejected claim ID: {}", updatedClaim.getId());

        // TODO: Trigger notification to claim.getUser() (the claimant who was rejected)
        // Example: notificationService.createClaimRejectedNotification(claim.getUser(), updatedClaim, rejectionReason);

        return convertToDTO(updatedClaim);
    }


    public ClaimDTO getClaimById(Long id) {
        return claimRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + id));
    }

    public List<ClaimDTO> getAllClaims() { // Should be admin restricted mostly
        return claimRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClaimDTO> getClaimsByItemId(Long itemId) { // For item owner to see claims on their item
        return claimRepository.findByItemId(itemId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ClaimDTO> getClaimsByUserId(Long userId) { // For a user to see claims they made
        return claimRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    // This method was used by ItemController directly setting item.claimed = true
    // It bypasses the detailed claim creation and approval process.
    // Consider deprecating or removing if all claims should go through ClaimService.createClaim & approveClaim
    @Transactional
    public void markItemAsDirectlyClaimed(Long itemId, Long claimingUserId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + itemId));
        User claimant = userRepository.findById(claimingUserId)
                .orElseThrow(() -> new RuntimeException("Claiming user not found with ID: " + claimingUserId));

        if (item.isClaimed()) {
            throw new IllegalStateException("Item with ID: " + itemId + " is already claimed.");
        }
        // This direct claim might be for when a user finds THEIR OWN "Lost" item listing,
        // or if an Admin directly resolves something.
        // The primary flow is a user claiming a "Found" item.
        // For this method, it might be better if it only applies to items the claimingUser reported as lost.
        if (!item.isLost() || !item.getUser().getId().equals(claimingUserId)) {
            log.warn("Direct claim attempt on item ID {} by user ID {} under invalid conditions.", itemId, claimingUserId);
            throw new IllegalStateException("This direct claim method is not applicable under these conditions.");
        }

        item.setClaimed(true);
        item.setClaimedByUser(claimant);
        item.setClaimedDate(LocalDateTime.now());
        itemRepository.save(item);
        log.info("Item ID: {} directly marked as claimed by its owner (User ID: {}) via ItemService.", itemId, claimingUserId);
    }
    @Transactional
    public ClaimDTO updateClaim(Long claimId, ClaimDTO claimDTO, Long currentUserId) {
        log.info("Attempting to update claim ID: {} by user ID: {}", claimId, currentUserId);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> {
                    log.error("Claim update failed: Claim not found with ID: {}", claimId);
                    return new RuntimeException("Claim not found with ID: " + claimId);
                });

        // Authorization: Only the user who created the claim can update it.
        if (!claim.getUser().getId().equals(currentUserId)) {
            log.warn("Claim update failed: User ID {} is not authorized to update claim ID {}.",
                    currentUserId, claimId);
            throw new SecurityException("You are not authorized to update this claim.");
        }

        // Logic: Only allow updates if the claim is still PENDING.
        if (claim.getStatus() != ClaimStatus.PENDING) {
            log.warn("Claim update failed: Claim ID {} is not in PENDING status (current status: {}). Cannot update.",
                    claimId, claim.getStatus());
            throw new IllegalStateException("Only pending claims can be updated. Current status: " + claim.getStatus());
        }

        // Fields that can be updated:
        if (claimDTO.getDescription() != null && !claimDTO.getDescription().isBlank()) {
            claim.setDescription(claimDTO.getDescription());
            log.info("Updated description for claim ID: {}", claimId);
        }

        // If you allow updating the proof image path via this DTO (controller would handle upload)
        if (claimDTO.getProofImagePath() != null) {
            // Note: If a new image is uploaded, the controller should have handled storing it
            // and setting this new path in the DTO. Consider deleting the old proof image if it changes.
            if (claim.getProofImagePath() != null && !claim.getProofImagePath().equals(claimDTO.getProofImagePath())) {
                // TODO: Consider deleting the old proof image file using FileStorageService
                // fileStorageService.delete(claim.getProofImagePath());
                log.info("Old proof image path {} for claim ID {} will be replaced.", claim.getProofImagePath(), claimId);
            }
            claim.setProofImagePath(claimDTO.getProofImagePath());
            log.info("Updated proofImagePath for claim ID: {}", claimId);
        }

        // Do NOT allow updating status, itemId, or userId via this general update method.
        // Those are handled by specific methods like approveClaim, rejectClaim, or re-assigning (if ever needed).

        Claim updatedClaim = claimRepository.save(claim);
        log.info("Successfully updated claim ID: {}", updatedClaim.getId());
        return convertToDTO(updatedClaim);
    }

    @Transactional
    public ClaimDTO forwardClaimToAdmin(Long claimId, Long finderUserId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        // Authorization: Ensure finderUserId is the user who reported the item
        if (!claim.getItem().getUser().getId().equals(finderUserId)) {
            throw new SecurityException("You are not authorized to forward this claim.");
        }
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new IllegalStateException("Only PENDING claims can be forwarded to admin. Current status: " + claim.getStatus());
        }

        claim.setStatus(ClaimStatus.FORWARDED_TO_ADMIN);
        // Optionally: set forwardedByUserId, forwardedAt if you add those fields to Claim model
        Claim updatedClaim = claimRepository.save(claim);
        log.info("Claim ID {} forwarded to admin by finder ID {}", claimId, finderUserId);

        // TODO: Notify Admin(s) that a claim needs review
        // notificationService.createAdminClaimReviewNotification(updatedClaim);

        return convertToDTO(updatedClaim);
    }
    @Transactional
    public void deleteClaim(Long id, Long currentUserId, String userRole) { // Added user context for authorization
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + id));

        // Authorization: Only the user who made the claim or an admin can delete it.
        // And only if it's in a state that allows deletion (e.g., PENDING).
        boolean isAdmin = "admin".equalsIgnoreCase(userRole); // Assuming "admin" role string
        boolean isClaimOwner = claim.getUser().getId().equals(currentUserId);

        if (!isClaimOwner && !isAdmin) {
            throw new SecurityException("You are not authorized to delete this claim.");
        }

        if (claim.getStatus() == ClaimStatus.APPROVED && !isAdmin) {
            // If a claim is approved, only an admin should be able to delete it,
            // as it might require un-claiming the item.
            throw new IllegalStateException("Cannot delete an approved claim. Contact an administrator.");
        }

        // If this was an approved claim, and an admin is deleting, we might need to un-claim the item.
        // However, deleting an approved claim is a complex operation.
        // For now, let's assume only PENDING claims can be easily deleted by owner.
        if (claim.getStatus() != ClaimStatus.PENDING && !isAdmin) {
            throw new IllegalStateException("Only pending claims can be deleted by the claim owner.");
        }

        // If an admin deletes an approved claim, revert item status
        if (isAdmin && claim.getStatus() == ClaimStatus.APPROVED && claim.getItem() != null && claim.getItem().isClaimed()) {
            Item item = claim.getItem();
            item.setClaimed(false);
            item.setClaimedByUser(null);
            item.setClaimedDate(null);
            itemRepository.save(item);
            log.info("Admin deleted approved claim ID: {}. Item ID: {} unmarked as claimed.", id, item.getId());
        }

        claimRepository.deleteById(id);
        log.info("Deleted claim ID: {} by User ID: {} (Role: {})", id, currentUserId, userRole);
    }

    private ClaimDTO convertToDTO(Claim claim) {
        return ClaimDTO.builder()
                .id(claim.getId())
                .description(claim.getDescription())
                .proofImagePath(claim.getProofImagePath())
                .status(claim.getStatus())
                .claimDate(claim.getClaimDate())
                .userId(claim.getUser() != null ? claim.getUser().getId() : null)
                .username(claim.getUser() != null ? claim.getUser().getUsername() : "N/A") // Add username
                .itemId(claim.getItem() != null ? claim.getItem().getId() : null)
                .build();
    }
    public List<ClaimDTO> getClaimsForAdminReview() {
        log.info("Fetching claims with status FORWARDED_TO_ADMIN for admin review.");
        return claimRepository.findByStatus(ClaimStatus.FORWARDED_TO_ADMIN).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}

// You'll also need to update ClaimRepository to include:
// boolean existsByUserIdAndItemIdAndStatus(Long userId, Long itemId, ClaimStatus status);
// List<Claim> findByItemIdAndStatus(Long itemId, ClaimStatus status);