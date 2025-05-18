package com.LostAndFound.LostAndFound.model;

public enum ClaimStatus {
    PENDING, // Claim submitted, awaiting review by the item poster (finder)
    FORWARDED_TO_ADMIN,
    APPROVED,   // Claim approved by the item poster (finder)
    REJECTED,   // Claim rejected by the item poster (finder)
    NEEDS_MORE_INFO, // Item poster requested more information (for future chat integration)
    DISPUTED,   // Claimant disputed a rejection, awaiting admin review (for future)
    CLOSED      // Claim process is finalized (e.g., item returned or claim withdrawn)
}