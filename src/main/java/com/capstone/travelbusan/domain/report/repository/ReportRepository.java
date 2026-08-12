package com.capstone.travelbusan.domain.report.repository;

import com.capstone.travelbusan.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByReporter_IdOrderByCreatedAtDesc(UUID reporterId);
}
