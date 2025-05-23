package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.ClaimDTO;
import com.LostAndFound.LostAndFound.model.Claim;
import com.LostAndFound.LostAndFound.model.Item;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.model.ClaimStatus;
import com.LostAndFound.LostAndFound.repository.ClaimRepository;
import com.LostAndFound.LostAndFound.repository.ItemRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
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
    // private final NotificationService notificationService; // TODO: Integrate later

    @Transactional
    public ClaimDTO createClaim(ClaimDTO claimDTO) {
        // ... (createClaim logic remains the same as in artifact claim_service_admin_auth_fix)
        log.info("Attempting to create claim for item ID: {} by user ID: {}", claimDTO.getItemId(), claimDTO.getUserId());
        User claimant = userRepository.findById(claimDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User (claimant) not found with ID: " + claimDTO.getUserId()));
        Item item = itemRepository.findById(claimDTO.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + claimDTO.getItemId()));

        if (item.getUser().getId().equals(claimant.getId())) {
            throw new IllegalStateException("You cannot claim an item you reported.");
        }
        if (item.isLost()) {
            throw new IllegalStateException("Claims can only be made on items reported as 'Found'.");
        }
        if (item.isClaimed()) {
            throw new IllegalStateException("This item has already been claimed by someone.");
        }
        if (claimRepository.existsByUserIdAndItemIdAndStatus(claimant.getId(), item.getId(), ClaimStatus.PENDING) ||
                claimRepository.existsByUserIdAndItemIdAndStatus(claimant.getId(), item.getId(), ClaimStatus.FORWARDED_TO_ADMIN)) {
            throw new IllegalStateException("You already have an active claim for this item.");
        }

        Claim claim = new Claim();
        claim.setDescription(claimDTO.getDescription());
        claim.setProofImagePath(claimDTO.getProofImagePath());
        claim.setStatus(ClaimStatus.PENDING);
        claim.setClaimDate(LocalDateTime.now());
        claim.setUser(claimant);
        claim.setItem(item);
        Claim savedClaim = claimRepository.save(claim);
        log.info("Successfully created claim ID: {} for item ID: {} by user ID: {}", savedClaim.getId(), item.getId(), claimant.getId());
        // TODO: Notify item finder
        return convertToDTO(savedClaim);
    }

    @Transactional
    public ClaimDTO approveClaim(Long claimId, Long adminUserId) { // performingUserId is now adminUserId
        log.info("Admin (User ID: {}) attempting to approve claim ID: {}", adminUserId, claimId);

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Admin user performing action not found with ID: " + adminUserId));

        // CORRECTED Authorization Check: Ensure the user performing action is an ADMIN
        if (!"admin".equalsIgnoreCase(adminUser.getRole())) {
            log.warn("Claim approval failed: User ID {} (Role: {}) is not an Admin.", adminUser.getId(), adminUser.getRole());
            throw new SecurityException("You are not authorized to approve this claim. Admin privileges required.");
        }

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        Item item = claim.getItem();
        if (item == null) throw new IllegalStateException("Claim is not associated with any item.");

        // Admin can approve a claim that was FORWARDED_TO_ADMIN (or PENDING, if direct admin action is allowed)
        if (claim.getStatus() != ClaimStatus.FORWARDED_TO_ADMIN && claim.getStatus() != ClaimStatus.PENDING) {
            log.warn("Claim approval failed: Claim ID {} is not in a state that can be approved by admin (current status: {}).", claimId, claim.getStatus());
            throw new IllegalStateException("This claim is not awaiting admin approval (current status: " + claim.getStatus() + ").");
        }

        if (item.isClaimed()) {
            log.warn("Claim approval failed: Item ID {} is already marked as claimed by another approved claim.", item.getId());
            throw new IllegalStateException("This item has already been claimed via another approved claim. Please review.");
        }

        claim.setStatus(ClaimStatus.APPROVED);
        item.setClaimed(true);
        item.setClaimedByUser(claim.getUser());
        item.setClaimedDate(LocalDateTime.now());
        itemRepository.save(item);

        List<Claim> otherActiveClaims = claimRepository.findByItemId(item.getId()).stream()
                .filter(c -> !c.getId().equals(claimId) &&
                        (c.getStatus() == ClaimStatus.PENDING || c.getStatus() == ClaimStatus.FORWARDED_TO_ADMIN))
                .toList();

        for (Claim otherClaim : otherActiveClaims) {
            otherClaim.setStatus(ClaimStatus.REJECTED);
            claimRepository.save(otherClaim);
            log.info("Admin approval of claim {} automatically rejected other claim ID: {} for item ID: {}", claimId, otherClaim.getId(), item.getId());
            // TODO: Notify these other claimants
        }

        Claim updatedClaim = claimRepository.save(claim);
        log.info("Admin (User ID: {}) successfully approved claim ID: {}. Item ID: {} now marked as claimed by user ID: {}",
                adminUserId, updatedClaim.getId(), item.getId(), item.getClaimedByUser().getId());
        // TODO: Notify successful claimant
        return convertToDTO(updatedClaim);
    }

    @Transactional
    public ClaimDTO rejectClaim(Long claimId, Long adminUserId, String rejectionReason) { // performingUserId is now adminUserId
        log.info("Admin (User ID: {}) attempting to reject claim ID: {}", adminUserId, claimId);

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Admin user performing action not found with ID: " + adminUserId));

        // CORRECTED Authorization Check: Ensure the user performing action is an ADMIN
        if (!"admin".equalsIgnoreCase(adminUser.getRole())) {
            log.warn("Claim rejection failed: User ID {} (Role: {}) is not an Admin.", adminUser.getId(), adminUser.getRole());
            throw new SecurityException("You are not authorized to reject this claim. Admin privileges required.");
        }

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        // Admin can reject a claim that was PENDING or FORWARDED_TO_ADMIN
        if (claim.getStatus() != ClaimStatus.FORWARDED_TO_ADMIN && claim.getStatus() != ClaimStatus.PENDING) {
            log.warn("Claim rejection failed: Claim ID {} is not in a state that can be rejected by admin (current status: {}).", claimId, claim.getStatus());
            throw new IllegalStateException("This claim is not awaiting admin decision (current status: " + claim.getStatus() + ").");
        }

        claim.setStatus(ClaimStatus.REJECTED);
        // TODO: Store rejectionReason in Claim entity

        Claim updatedClaim = claimRepository.save(claim);
        log.info("Admin (User ID: {}) successfully rejected claim ID: {}", adminUserId, updatedClaim.getId());
        // TODO: Notify rejected claimant
        return convertToDTO(updatedClaim);
    }

    @Transactional
    public ClaimDTO forwardClaimToAdmin(Long claimId, Long finderUserId) {
        // ... (forwardClaimToAdmin logic remains the same as in artifact claim_service_admin_auth_fix)
        log.info("Finder (User ID {}) attempting to forward claim ID: {}", finderUserId, claimId);
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));
        if (!claim.getItem().getUser().getId().equals(finderUserId)) {
            throw new SecurityException("You are not authorized to forward this claim (must be item reporter).");
        }
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new IllegalStateException("Only PENDING claims can be forwarded to admin. Current status: " + claim.getStatus());
        }
        claim.setStatus(ClaimStatus.FORWARDED_TO_ADMIN);
        Claim updatedClaim = claimRepository.save(claim);
        log.info("Claim ID {} forwarded to admin by finder ID {}", claimId, finderUserId);
        // TODO: Notify Admin(s)
        return convertToDTO(updatedClaim);
    }

    public ClaimDTO getClaimById(Long id) {
        return claimRepository.findById(id).map(this::convertToDTO).orElseThrow(() -> new RuntimeException("Claim not found with ID: " + id));
    }
    public List<ClaimDTO> getAllClaims() {
        return claimRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    public List<ClaimDTO> getClaimsByItemId(Long itemId) {
        return claimRepository.findByItemId(itemId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    public List<ClaimDTO> getClaimsByUserId(Long userId) {
        return claimRepository.findByUserId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    @Transactional
    public ClaimDTO updateClaim(Long claimId, ClaimDTO claimDTO, Long currentUserId) {
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));
        if (!claim.getUser().getId().equals(currentUserId)) { throw new SecurityException("You are not authorized to update this claim."); }
        if (claim.getStatus() != ClaimStatus.PENDING) { throw new IllegalStateException("Only pending claims can be updated. Current status: " + claim.getStatus());}
        if (claimDTO.getDescription() != null && !claimDTO.getDescription().isBlank()) claim.setDescription(claimDTO.getDescription());
        if (claimDTO.getProofImagePath() != null) claim.setProofImagePath(claimDTO.getProofImagePath());
        return convertToDTO(claimRepository.save(claim));
    }
    @Transactional
    public void deleteClaim(Long id, Long currentUserId, String userRole) {
        Claim claim = claimRepository.findById(id).orElseThrow(() -> new RuntimeException("Claim not found: " + id));
        boolean isAdmin = "admin".equalsIgnoreCase(userRole);
        boolean isClaimOwner = claim.getUser().getId().equals(currentUserId);
        if (!isClaimOwner && !isAdmin) throw new SecurityException("You are not authorized to delete this claim.");
        if (claim.getStatus() == ClaimStatus.APPROVED && !isAdmin) throw new IllegalStateException("Cannot delete an approved claim. Contact an administrator.");
        if (claim.getStatus() != ClaimStatus.PENDING && !isAdmin && claim.getStatus() != ClaimStatus.FORWARDED_TO_ADMIN) { // Admin might delete forwarded claims
            throw new IllegalStateException("Only pending or forwarded claims can be deleted by the claim owner/admin in this flow.");
        }
        if (isAdmin && claim.getStatus() == ClaimStatus.APPROVED && claim.getItem() != null && claim.getItem().isClaimed()) {
            Item item = claim.getItem(); item.setClaimed(false); item.setClaimedByUser(null); item.setClaimedDate(null); itemRepository.save(item);
        }
        claimRepository.deleteById(id);
        log.info("Deleted claim ID: {} by User ID: {} (Role: {})", id, currentUserId, userRole);
    }
    public List<ClaimDTO> getClaimsForAdminReview() {
        log.info("Fetching claims with status FORWARDED_TO_ADMIN for admin review.");
        return claimRepository.findByStatus(ClaimStatus.FORWARDED_TO_ADMIN).stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    private ClaimDTO convertToDTO(Claim claim) {
        return ClaimDTO.builder().id(claim.getId()).description(claim.getDescription()).proofImagePath(claim.getProofImagePath()).status(claim.getStatus()).claimDate(claim.getClaimDate()).userId(claim.getUser() != null ? claim.getUser().getId() : null).username(claim.getUser() != null ? claim.getUser().getUsername() : "N/A").itemId(claim.getItem() != null ? claim.getItem().getId() : null).build();
    }
}
