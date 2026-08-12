package com.capstone.travelbusan.domain.guideconversion.repository;

import com.capstone.travelbusan.domain.guideconversion.entity.GuideApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuideApplicationRepository extends JpaRepository<GuideApplication, UUID> {

    List<GuideApplication> findByApplicant_IdOrderByAppliedAtDesc(UUID applicantId);

    Optional<GuideApplication> findFirstByApplicant_IdOrderByAppliedAtDesc(UUID applicantId);

    boolean existsByApplicant_IdAndStatus(UUID applicantId, String status);
}
