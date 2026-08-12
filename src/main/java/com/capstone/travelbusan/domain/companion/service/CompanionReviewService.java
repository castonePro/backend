package com.capstone.travelbusan.domain.companion.service;

import com.capstone.travelbusan.domain.companion.dto.CompanionReviewDto;
import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.companion.entity.CompanionApplication;
import com.capstone.travelbusan.domain.companion.entity.CompanionReview;
import com.capstone.travelbusan.domain.companion.repository.CompanionApplicationRepository;
import com.capstone.travelbusan.domain.companion.repository.CompanionRepository;
import com.capstone.travelbusan.domain.companion.repository.CompanionReviewRepository;
import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanionReviewService {

    private static final int DOUBLE_BLIND_REVEAL_DAYS = 7;

    private final CompanionReviewRepository reviewRepository;
    private final CompanionRepository companionRepository;
    private final CompanionApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    // 1. 리뷰 작성 — 완료된 동행의 참여자(방장 포함)만, 서로에게 한 번씩
    @Transactional
    public CompanionReviewDto.Response submitReview(UUID reviewerId, UUID companionId, CompanionReviewDto.CreateRequest request) {
        Companion companion = findCompanion(companionId);
        if (!Companion.STATUS_COMPLETED.equals(companion.getStatus())) {
            throw new IllegalArgumentException("완료된 동행만 평가할 수 있습니다.");
        }
        if (request.getRevieweeId() == null || reviewerId.equals(request.getRevieweeId())) {
            throw new IllegalArgumentException("평가 대상이 올바르지 않습니다.");
        }
        if (request.getRating() == null
                || request.getRating().compareTo(BigDecimal.ONE) < 0
                || request.getRating().compareTo(new BigDecimal("5.0")) > 0) {
            throw new IllegalArgumentException("평점은 1.0 ~ 5.0 사이여야 합니다.");
        }

        User reviewer = findUser(reviewerId);
        User reviewee = findUser(request.getRevieweeId());

        if (!isTripMember(companion, reviewerId)) {
            throw new IllegalArgumentException("해당 동행의 참여자만 평가할 수 있습니다.");
        }
        boolean revieweeIsHost = companion.getHost().getId().equals(reviewee.getId());
        if (!revieweeIsHost && !isTripMember(companion, reviewee.getId())) {
            throw new IllegalArgumentException("평가 대상이 해당 동행의 참여자가 아닙니다.");
        }
        if (reviewRepository.existsByCompanion_CompanionIdAndReviewer_IdAndReviewee_Id(
                companionId, reviewerId, reviewee.getId())) {
            throw new IllegalArgumentException("이미 이 사람에 대한 평가를 남겼습니다.");
        }

        // 방장이 아닌 사람에게는 "일정 운영 평가"를 매길 수 없다.
        BigDecimal operationRating = revieweeIsHost ? request.getOperationRating() : null;
        if (operationRating != null
                && (operationRating.compareTo(BigDecimal.ONE) < 0 || operationRating.compareTo(new BigDecimal("5.0")) > 0)) {
            throw new IllegalArgumentException("운영 평가 점수는 1.0 ~ 5.0 사이여야 합니다.");
        }

        CompanionReview review = CompanionReview.builder()
                .companion(companion)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.getRating())
                .tags(request.getTags())
                .operationRating(operationRating)
                .comment(request.getComment())
                .build();

        CompanionReview saved = reviewRepository.save(review);

        // 내용은 공개하지 않고, 평가가 도착했다는 사실만 알린다 (더블 블라인드 유지)
        fcmService.sendNotification(
                reviewee.getId(),
                "새 동행 평가 도착",
                "'" + companion.getTitle() + "' 동행에서 새로운 평가를 받았어요."
        );

        return CompanionReviewDto.Response.from(saved);
    }

    // 2. 내가 평가할 수 있는 대상 목록 (완료된 동행의 다른 참여자들, 이미 평가했는지 여부 포함)
    public List<CompanionReviewDto.ReviewableMember> getReviewableMembers(UUID userId, UUID companionId) {
        Companion companion = findCompanion(companionId);
        if (!isTripMember(companion, userId)) {
            throw new IllegalArgumentException("해당 동행의 참여자만 조회할 수 있습니다.");
        }

        List<CompanionReviewDto.ReviewableMember> members = new java.util.ArrayList<>();

        // 방장 (본인이 방장이 아닐 때만 대상에 포함)
        User host = companion.getHost();
        if (!host.getId().equals(userId)) {
            members.add(toReviewableMember(companionId, userId, host, true));
        }

        // 승인된 참여자들 (본인 제외)
        applicationRepository.findByCompanion_CompanionIdOrderByCreatedAtDesc(companionId).stream()
                .filter(app -> CompanionApplication.STATUS_APPROVED.equals(app.getStatus()))
                .map(CompanionApplication::getApplicant)
                .filter(applicant -> !applicant.getId().equals(userId))
                .forEach(applicant -> members.add(toReviewableMember(companionId, userId, applicant, false)));

        return members;
    }

    // 3. 특정 동행에서 내가 받은 리뷰 — 더블 블라인드 공개분만 반환 + 대기 개수
    public CompanionReviewDto.ReceivedList getReceivedReviews(UUID userId, UUID companionId) {
        Companion companion = findCompanion(companionId);
        List<CompanionReview> all = reviewRepository.findByCompanion_CompanionIdAndReviewee_Id(companionId, userId);

        List<CompanionReviewDto.Response> revealed = new java.util.ArrayList<>();
        int pending = 0;
        for (CompanionReview review : all) {
            if (isRevealed(companion, review)) {
                revealed.add(CompanionReviewDto.Response.from(review));
            } else {
                pending++;
            }
        }
        return CompanionReviewDto.ReceivedList.builder()
                .reviews(revealed)
                .pendingCount(pending)
                .build();
    }

    // 4. 마이페이지용 동행 이력·평가 요약 (전체 동행 기준)
    public CompanionReviewDto.Summary getMySummary(UUID userId) {
        User user = findUser(userId);
        List<CompanionReview> received = reviewRepository.findByReviewee_Id(userId);

        List<BigDecimal> revealedRatings = new java.util.ArrayList<>();
        List<BigDecimal> revealedOperationRatings = new java.util.ArrayList<>();
        for (CompanionReview review : received) {
            if (isRevealed(review.getCompanion(), review)) {
                revealedRatings.add(review.getRating());
                if (review.getOperationRating() != null) {
                    revealedOperationRatings.add(review.getOperationRating());
                }
            }
        }

        return CompanionReviewDto.Summary.builder()
                .companionHostCount(user.getCompanionHostCount())
                .companionJoinCount(user.getCompanionJoinCount())
                .averageRating(average(revealedRatings))
                .operationAverageRating(average(revealedOperationRatings))
                .reviewCount(revealedRatings.size())
                .build();
    }

    // ==================== 내부 유틸 ====================

    private CompanionReviewDto.ReviewableMember toReviewableMember(UUID companionId, UUID viewerId, User target, boolean isHost) {
        boolean alreadyReviewed = reviewRepository.existsByCompanion_CompanionIdAndReviewer_IdAndReviewee_Id(
                companionId, viewerId, target.getId());
        return CompanionReviewDto.ReviewableMember.builder()
                .userId(target.getId().toString())
                .nickname(target.getNickname())
                .profileImageUrl(target.getProfileImageUrl())
                .isHost(isHost)
                .alreadyReviewed(alreadyReviewed)
                .build();
    }

    // 쌍방 모두 제출했거나, 완료 시점으로부터 7일이 지나면 공개
    private boolean isRevealed(Companion companion, CompanionReview review) {
        boolean reciprocal = reviewRepository.existsByCompanion_CompanionIdAndReviewer_IdAndReviewee_Id(
                companion.getCompanionId(), review.getReviewee().getId(), review.getReviewer().getId());
        if (reciprocal) {
            return true;
        }
        LocalDateTime completedAt = companion.getCompletedAt();
        return completedAt != null && completedAt.plusDays(DOUBLE_BLIND_REVEAL_DAYS).isBefore(LocalDateTime.now());
    }

    private boolean isTripMember(Companion companion, UUID userId) {
        if (companion.getHost().getId().equals(userId)) {
            return true;
        }
        return applicationRepository.findByCompanion_CompanionIdAndApplicant_Id(companion.getCompanionId(), userId)
                .map(app -> CompanionApplication.STATUS_APPROVED.equals(app.getStatus()))
                .orElse(false);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    private Companion findCompanion(UUID companionId) {
        return companionRepository.findById(companionId)
                .orElseThrow(() -> new IllegalArgumentException("동행을 찾을 수 없습니다."));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
