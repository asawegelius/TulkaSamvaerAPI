package com.tulka.samvaer.sections.events.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.tulka.samvaer.sections.events.domain.BringItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BringItemRepository extends JpaRepository<BringItem, UUID> {

    /**
     * Locks the item row so two members cannot take the last slot at the same time.
     * Call before the item's claims are read in the transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from BringItem i where i.id = :itemId and i.event.id = :eventId")
    Optional<BringItem> lockForClaim(@Param("eventId") UUID eventId, @Param("itemId") UUID itemId);
}
