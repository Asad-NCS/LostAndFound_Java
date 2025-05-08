package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.ClaimDTO;
import com.LostAndFound.LostAndFound.model.Claim;
import com.LostAndFound.LostAndFound.model.Item;
import com.LostAndFound.LostAndFound.model.User;
import com.LostAndFound.LostAndFound.repository.ClaimRepository;
import com.LostAndFound.LostAndFound.repository.ItemRepository;
import com.LostAndFound.LostAndFound.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimService {
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public ClaimDTO createClaim(ClaimDTO claimDTO) {
        User user = userRepository.findById(claimDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = itemRepository.findById(claimDTO.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Claim claim = new Claim();
        claim.setDescription(claimDTO.getDescription());
        claim.setApproved(false); // Default to false
        claim.setClaimDate(LocalDateTime.now());
        claim.setUser(user);
        claim.setItem(item);

        Claim savedClaim = claimRepository.save(claim);
        return convertToDTO(savedClaim);
    }

    public ClaimDTO getClaimById(Long id) {
        return claimRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
    }

    public List<ClaimDTO> getAllClaims() {
        return claimRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ClaimDTO updateClaim(Long id, ClaimDTO claimDTO) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        claim.setDescription(claimDTO.getDescription());
        claim.setApproved(claimDTO.isApproved());

        Claim updatedClaim = claimRepository.save(claim);
        return convertToDTO(updatedClaim);
    }

    public void deleteClaim(Long id) {
        claimRepository.deleteById(id);
    }

    private ClaimDTO convertToDTO(Claim claim) {
        ClaimDTO dto = new ClaimDTO();
        dto.setId(claim.getId());
        dto.setDescription(claim.getDescription());
        dto.setApproved(claim.isApproved());
        dto.setUserId(claim.getUser() != null ? claim.getUser().getId() : null);
        dto.setItemId(claim.getItem() != null ? claim.getItem().getId() : null);
        return dto;
    }
}