package com.capstone.travelbusan.domain.report.service;

import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.companion.repository.CompanionRepository;
import com.capstone.travelbusan.domain.report.dto.ReportDto;
import com.capstone.travelbusan.domain.report.entity.Report;
import com.capstone.travelbusan.domain.report.repository.ReportRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<String> VALID_REASONS = Set.of(
            Report.REASON_NO_SHOW,
            Report.REASON_HARASSMENT,
            Report.REASON_INAPPROPRIATE_BEHAVIOR,
            Report.REASON_FRAUD,
            Report.REASON_OTHER
    );

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CompanionRepository companionRepository;

    @Transactional
    public ReportDto.Response submitReport(UUID reporterId, ReportDto.CreateRequest request) {
        if (request.getReportedUserId() == null || reporterId.equals(request.getReportedUserId())) {
            throw new IllegalArgumentException("신고 대상이 올바르지 않습니다.");
        }
        if (request.getReasonCategory() == null || !VALID_REASONS.contains(request.getReasonCategory())) {
            throw new IllegalArgumentException("신고 사유가 올바르지 않습니다.");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        User reportedUser = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new IllegalArgumentException("신고 대상을 찾을 수 없습니다."));

        Companion companion = null;
        if (request.getCompanionId() != null) {
            companion = companionRepository.findById(request.getCompanionId())
                    .orElseThrow(() -> new IllegalArgumentException("동행을 찾을 수 없습니다."));
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .companion(companion)
                .reasonCategory(request.getReasonCategory())
                .description(request.getDescription())
                .build();

        return ReportDto.Response.from(reportRepository.save(report));
    }

    public List<ReportDto.Response> getMyReports(UUID reporterId) {
        return reportRepository.findByReporter_IdOrderByCreatedAtDesc(reporterId).stream()
                .map(ReportDto.Response::from)
                .toList();
    }
}
