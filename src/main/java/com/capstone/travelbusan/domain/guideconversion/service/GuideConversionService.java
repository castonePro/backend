package com.capstone.travelbusan.domain.guideconversion.service;

import com.capstone.travelbusan.domain.companion.dto.CompanionReviewDto;
import com.capstone.travelbusan.domain.companion.service.CompanionReviewService;
import com.capstone.travelbusan.domain.guideconversion.dto.GuideConversionDto;
import com.capstone.travelbusan.domain.guideconversion.entity.GuideApplication;
import com.capstone.travelbusan.domain.guideconversion.repository.GuideApplicationRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuideConversionService {

    private static final int HOST_REQUIREMENT = 3;
    private static final int JOIN_REQUIREMENT = 3;
    private static final BigDecimal RATING_REQUIREMENT = new BigDecimal("4.0");

    private final GuideApplicationRepository guideApplicationRepository;
    private final UserRepository userRepository;
    private final CompanionReviewService companionReviewService;

    // 전환 진행 현황 조회 — 처음으로 조건을 모두 만족한 시점에 예비 가이드 배지를 부여한다.
    @Transactional
    public GuideConversionDto.StatusResponse getStatus(UUID userId) {
        User user = findUser(userId);
        CompanionReviewDto.Summary summary = companionReviewService.getMySummary(userId);

        boolean meetsHost = user.getCompanionHostCount() >= HOST_REQUIREMENT;
        boolean meetsJoin = user.getCompanionJoinCount() >= JOIN_REQUIREMENT;
        boolean meetsRating = summary.getOperationAverageRating() != null
                && summary.getOperationAverageRating().compareTo(RATING_REQUIREMENT) >= 0;
        boolean eligible = meetsHost && meetsJoin && meetsRating;

        if (eligible && !user.isPreliminaryGuide()) {
            user.grantPreliminaryGuide();
        }

        GuideApplication latest = guideApplicationRepository
                .findFirstByApplicant_IdOrderByAppliedAtDesc(userId).orElse(null);

        return GuideConversionDto.StatusResponse.builder()
                .companionHostCount(user.getCompanionHostCount())
                .companionJoinCount(user.getCompanionJoinCount())
                .operationAverageRating(summary.getOperationAverageRating())
                .meetsHostRequirement(meetsHost)
                .meetsJoinRequirement(meetsJoin)
                .meetsRatingRequirement(meetsRating)
                .eligible(eligible)
                .preliminaryGuide(user.isPreliminaryGuide())
                .alreadyGuide(user.isGuide())
                .latestApplication(latest != null ? GuideConversionDto.ApplicationResponse.from(latest) : null)
                .build();
    }

    // 정식 가이드 전환 신청 — 예비 가이드 조건을 만족해야 하고, 대기중인 신청이 없어야 한다.
    @Transactional
    public GuideConversionDto.ApplicationResponse apply(UUID userId, GuideConversionDto.ApplyRequest request) {
        User user = findUser(userId);
        GuideConversionDto.StatusResponse status = getStatus(userId); // 최신 조건 재계산 + 배지 부여

        if (status.isAlreadyGuide()) {
            throw new IllegalArgumentException("이미 가이드로 등록된 사용자입니다.");
        }
        if (!status.isEligible()) {
            throw new IllegalArgumentException("정식 가이드 전환 조건(참여 3회·방장 3회·운영 평가 4.0 이상)을 아직 만족하지 않았습니다.");
        }
        if (guideApplicationRepository.existsByApplicant_IdAndStatus(userId, GuideApplication.STATUS_PENDING)) {
            throw new IllegalArgumentException("이미 심사 대기중인 신청이 있습니다.");
        }

        GuideApplication application = GuideApplication.builder()
                .applicant(user)
                .message(request.getMessage())
                .build();

        return GuideConversionDto.ApplicationResponse.from(guideApplicationRepository.save(application));
    }

    public List<GuideConversionDto.ApplicationResponse> getMyApplications(UUID userId) {
        return guideApplicationRepository.findByApplicant_IdOrderByAppliedAtDesc(userId).stream()
                .map(GuideConversionDto.ApplicationResponse::from)
                .toList();
    }

    // 운영자 심사 처리.
    // 주의: 이 프로젝트에는 아직 관리자 role/인증 체계가 없어 로그인한 사용자면 누구나 호출할 수 있다.
    // 프로덕션 반영 전 반드시 관리자 권한 검증을 추가할 것 (report 도메인과 동일한 한계).
    @Transactional
    public GuideConversionDto.ApplicationResponse review(UUID applicationId, GuideConversionDto.ReviewRequest request) {
        GuideApplication application = guideApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));

        if ("APPROVE".equalsIgnoreCase(request.getDecision())) {
            application.approve(request.getNote());
        } else if ("REJECT".equalsIgnoreCase(request.getDecision())) {
            application.reject(request.getNote());
        } else {
            throw new IllegalArgumentException("심사 결정 값이 올바르지 않습니다. (APPROVE 또는 REJECT)");
        }

        return GuideConversionDto.ApplicationResponse.from(application);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
