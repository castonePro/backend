package com.capstone.travelbusan.domain.payment.repository;

import com.capstone.travelbusan.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByPayer_IdOrderByCreatedAtDesc(UUID payerId);

    List<Payment> findByCompanion_CompanionIdAndTypeAndStatus(UUID companionId, String type, String status);

    Optional<Payment> findByCompanion_CompanionIdAndPayer_IdAndType(UUID companionId, UUID payerId, String type);

    boolean existsByCompanion_CompanionIdAndPayer_IdAndType(UUID companionId, UUID payerId, String type);
}
