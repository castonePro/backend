package com.capstone.travelbusan.domain.planner.repository;

import com.capstone.travelbusan.domain.planner.entity.PlannerSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlannerSessionRepository extends JpaRepository<PlannerSession, UUID> {
}
