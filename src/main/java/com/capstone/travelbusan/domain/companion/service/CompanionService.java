package com.capstone.travelbusan.domain.companion.service;

import com.capstone.travelbusan.domain.companion.dto.CompanionApplicationDto;
import com.capstone.travelbusan.domain.companion.dto.CompanionDto;
import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.companion.entity.CompanionApplication;
import com.capstone.travelbusan.domain.companion.repository.CompanionApplicationRepository;
import com.capstone.travelbusan.domain.companion.repository.CompanionRepository;
import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.domain.payment.dto.PaymentDto;
import com.capstone.travelbusan.domain.payment.service.PaymentService;
import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.planner.repository.ItineraryRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanionService {

    // 여행 시작 48시간 이내(또는 이미 시작한 뒤) 확정된 참여를 취소하면 노쇼로 기록한다.
    private static final int NO_SHOW_WINDOW_HOURS = 48;

    private final CompanionRepository companionRepository;
    private final CompanionApplicationRepository applicationRepository;
    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final PaymentService paymentService;

    // ==================== 방장 ====================

    // 1. 동행 모집하기 — 본인 인증 완료자만 가능
    @Transactional
    public CompanionDto.Response createCompanion(UUID hostId, CompanionDto.CreateRequest request) {
        User host = findUser(hostId);
        requirePhoneVerified(host);
        requireNotSanctioned(host);

        Itinerary itinerary = itineraryRepository.findById(request.getItineraryId())
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));
        if (!itinerary.getUserId().equals(hostId)) {
            throw new IllegalArgumentException("본인의 일정만 동행 모집에 사용할 수 있습니다.");
        }
        if (request.getMinParticipants() == null || request.getMaxParticipants() == null
                || request.getMinParticipants() < 1 || request.getMaxParticipants() < request.getMinParticipants()) {
            throw new IllegalArgumentException("모집 인원 설정이 올바르지 않습니다.");
        }

        Companion companion = Companion.builder()
                .itinerary(itinerary)
                .host(host)
                .title(request.getTitle())
                .minParticipants(request.getMinParticipants())
                .maxParticipants(request.getMaxParticipants())
                .preferenceTags(request.getPreferenceTags())
                .costSharingNote(request.getCostSharingNote())
                .description(request.getDescription())
                .minAge(request.getMinAge())
                .maxAge(request.getMaxAge())
                .snsHandle(request.getSnsHandle())
                .startDate(itinerary.getStartDate())
                .endDate(itinerary.getEndDate())
                .build();

        Companion saved = companionRepository.save(companion);
        return CompanionDto.Response.from(saved, 0);
    }

    // 2. 방장 수동 마감 — 정원 미달 여부에 따라 CONFIRMED 또는 UNDER_MINIMUM 전이
    @Transactional
    public CompanionDto.Response closeForApplications(UUID hostId, UUID companionId) {
        Companion companion = findCompanionAsHost(hostId, companionId);
        long approvedCount = applicationRepository
                .countByCompanion_CompanionIdAndStatus(companionId, CompanionApplication.STATUS_APPROVED);

        companion.closeForApplications((int) approvedCount);

        if (Companion.STATUS_CONFIRMED.equals(companion.getStatus())) {
            notifyConfirmedParticipants(companion, "동행이 확정되었습니다: " + companion.getTitle());
            // 확정된 참여자 전원에게 보증금 결제 요청 생성 (Phase 7)
            applicationRepository.findByCompanion_CompanionIdOrderByCreatedAtDesc(companionId).stream()
                    .filter(app -> CompanionApplication.STATUS_APPROVED.equals(app.getStatus()))
                    .forEach(app -> paymentService.requestDeposit(app.getApplicant(), companion));
        }
        return CompanionDto.Response.from(companion, approvedCount);
    }

    // 3. 최소 인원 미달 시 방장 결정: 소규모 진행 / 연기 / 취소
    @Transactional
    public CompanionDto.Response applyUnderMinimumDecision(UUID hostId, UUID companionId, CompanionDto.DecisionRequest request) {
        Companion companion = findCompanionAsHost(hostId, companionId);
        companion.applyUnderMinimumDecision(request.getDecision(), request.getNewStartDate(), request.getNewEndDate());

        long approvedCount = applicationRepository
                .countByCompanion_CompanionIdAndStatus(companionId, CompanionApplication.STATUS_APPROVED);

        String message = switch (request.getDecision()) {
            case "CONTINUE" -> "방장이 소규모로 진행하기로 결정했습니다: " + companion.getTitle();
            case "POSTPONE" -> "일정이 연기되었습니다: " + companion.getTitle();
            case "CANCEL" -> "동행이 취소되었습니다: " + companion.getTitle();
            default -> "동행 상태가 변경되었습니다: " + companion.getTitle();
        };
        notifyConfirmedParticipants(companion, message);

        // 소규모 진행으로 확정된 경우 보증금 결제 요청, 취소된 경우 전액 환급 (Phase 7)
        if ("CONTINUE".equals(request.getDecision())) {
            applicationRepository.findByCompanion_CompanionIdOrderByCreatedAtDesc(companionId).stream()
                    .filter(app -> CompanionApplication.STATUS_APPROVED.equals(app.getStatus()))
                    .forEach(app -> paymentService.requestDeposit(app.getApplicant(), companion));
        } else if ("CANCEL".equals(request.getDecision())) {
            paymentService.refundAllOnCancel(companion);
        }

        return CompanionDto.Response.from(companion, approvedCount);
    }

    @Transactional
    public CompanionDto.Response start(UUID hostId, UUID companionId) {
        Companion companion = findCompanionAsHost(hostId, companionId);
        companion.start();
        return CompanionDto.Response.from(companion, currentApprovedCount(companionId));
    }

    @Transactional
    public CompanionDto.Response complete(UUID hostId, UUID companionId) {
        Companion companion = findCompanionAsHost(hostId, companionId);
        companion.complete();

        // 동행 이력 카운트 갱신 — 가이드 전환 심사(Phase 6)의 기초 자료
        companion.getHost().incrementCompanionHostCount();
        applicationRepository.findByCompanion_CompanionIdOrderByCreatedAtDesc(companionId).stream()
                .filter(app -> CompanionApplication.STATUS_APPROVED.equals(app.getStatus()))
                .forEach(app -> app.getApplicant().incrementCompanionJoinCount());

        // 여행 완료 — 보증금 전액 환급 (Phase 7)
        paymentService.refundDepositsOnComplete(companion);

        return CompanionDto.Response.from(companion, currentApprovedCount(companionId));
    }

    @Transactional
    public CompanionDto.Response cancel(UUID hostId, UUID companionId) {
        Companion companion = findCompanionAsHost(hostId, companionId);
        companion.cancel();
        notifyConfirmedParticipants(companion, "방장이 동행을 취소했습니다: " + companion.getTitle());
        // 방장 취소 — 참여자 잘못이 아니므로 보증금·수수료 전액 환급 (Phase 7)
        paymentService.refundAllOnCancel(companion);
        return CompanionDto.Response.from(companion, currentApprovedCount(companionId));
    }

    // 신청자 관리 (방장용)
    public List<CompanionApplicationDto.Response> getApplications(UUID hostId, UUID companionId) {
        findCompanionAsHost(hostId, companionId); // 소유권 검증
        return applicationRepository.findByCompanion_CompanionIdOrderByCreatedAtDesc(companionId).stream()
                .map(CompanionApplicationDto.Response::from)
                .toList();
    }

    @Transactional
    public CompanionApplicationDto.Response approveApplication(UUID hostId, UUID applicationId) {
        CompanionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));
        verifyHost(hostId, application.getCompanion());

        long approvedCount = applicationRepository.countByCompanion_CompanionIdAndStatus(
                application.getCompanion().getCompanionId(), CompanionApplication.STATUS_APPROVED);
        if (approvedCount >= application.getCompanion().getMaxParticipants()) {
            throw new IllegalArgumentException("이미 모집 정원이 가득 찼습니다.");
        }

        application.approve();
        fcmService.sendNotification(
                application.getApplicant().getId(),
                "동행 참여 승인",
                application.getCompanion().getTitle() + " 동행 참여가 승인되었습니다."
        );
        // 3회차 이상 참여자에게 참여 수수료 결제 요청 (Phase 7)
        paymentService.requestParticipationFeeIfApplicable(application.getApplicant(), application.getCompanion());
        return CompanionApplicationDto.Response.from(application);
    }

    @Transactional
    public CompanionApplicationDto.Response rejectApplication(UUID hostId, UUID applicationId) {
        CompanionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));
        verifyHost(hostId, application.getCompanion());

        application.reject();
        fcmService.sendNotification(
                application.getApplicant().getId(),
                "동행 참여 거절",
                application.getCompanion().getTitle() + " 동행 참여 신청이 거절되었습니다."
        );
        return CompanionApplicationDto.Response.from(application);
    }

    public List<CompanionDto.Response> getMyHosting(UUID hostId) {
        return companionRepository.findByHost_IdOrderByCreatedAtDesc(hostId).stream()
                .map(c -> CompanionDto.Response.from(c, currentApprovedCount(c.getCompanionId())))
                .toList();
    }

    // ==================== 참여자 ====================

    // 동행 탐색 — 모집중인 목록 (부스트된 모집글을 상단에 노출, Phase 7)
    public List<CompanionDto.Response> getRecruiting() {
        return companionRepository.findByStatusOrderByCreatedAtDesc(Companion.STATUS_RECRUITING).stream()
                .sorted(Comparator.comparing((Companion c) -> !c.isBoosted())
                        .thenComparing(Companion::getCreatedAt, Comparator.reverseOrder()))
                .map(c -> CompanionDto.Response.from(c, currentApprovedCount(c.getCompanionId())))
                .toList();
    }

    // 방장이 모집글 부스트 결제 (Phase 7)
    @Transactional
    public PaymentDto.Response boost(UUID hostId, UUID companionId) {
        Companion companion = findCompanionAsHost(hostId, companionId);
        return paymentService.boost(companion.getHost(), companion);
    }

    public CompanionDto.Response getDetail(UUID companionId) {
        Companion companion = companionRepository.findById(companionId)
                .orElseThrow(() -> new IllegalArgumentException("모집글을 찾을 수 없습니다."));
        return CompanionDto.Response.from(companion, currentApprovedCount(companionId));
    }

    // 참여 신청 — 본인 인증 완료자만 가능
    @Transactional
    public CompanionApplicationDto.Response apply(UUID applicantId, UUID companionId, CompanionApplicationDto.CreateRequest request) {
        User applicant = findUser(applicantId);
        requirePhoneVerified(applicant);
        requireNotSanctioned(applicant);

        Companion companion = companionRepository.findById(companionId)
                .orElseThrow(() -> new IllegalArgumentException("모집글을 찾을 수 없습니다."));
        if (!companion.isRecruiting()) {
            throw new IllegalArgumentException("이미 마감된 모집글입니다.");
        }
        if (companion.getHost().getId().equals(applicantId)) {
            throw new IllegalArgumentException("본인이 개설한 동행에는 신청할 수 없습니다.");
        }
        if (applicationRepository.existsByCompanion_CompanionIdAndApplicant_IdAndStatusIn(
                companionId, applicantId,
                List.of(CompanionApplication.STATUS_PENDING, CompanionApplication.STATUS_APPROVED))) {
            throw new IllegalArgumentException("이미 신청한 모집글입니다.");
        }

        CompanionApplication application = CompanionApplication.builder()
                .companion(companion)
                .applicant(applicant)
                .introduction(request.getIntroduction())
                .build();

        CompanionApplication saved = applicationRepository.save(application);

        fcmService.sendNotification(
                companion.getHost().getId(),
                "새 동행 신청",
                applicant.getNickname() + "님이 '" + companion.getTitle() + "'에 참여를 신청했습니다."
        );

        return CompanionApplicationDto.Response.from(saved);
    }

    @Transactional
    public void cancelApplication(UUID applicantId, UUID applicationId) {
        CompanionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));
        if (!application.getApplicant().getId().equals(applicantId)) {
            throw new IllegalArgumentException("본인의 신청만 취소할 수 있습니다.");
        }

        boolean hadConfirmedSeat = CompanionApplication.STATUS_APPROVED.equals(application.getStatus());
        Companion companion = application.getCompanion();
        application.cancel();

        // 확정된 자리를 출발 임박(48시간 이내)이나 출발 이후에 취소하면 노쇼로 기록한다.
        if (hadConfirmedSeat && isWithinNoShowWindow(companion)) {
            application.markNoShow();
            application.getApplicant().recordNoShow();
            // 노쇼 확정 — 결제된 보증금 몰수 (Phase 7)
            paymentService.forfeitDepositOnNoShow(companion, application.getApplicant());
        }
    }

    // 방장이 실제로 나타나지 않은 참여자를 노쇼로 수동 처리 (취소 없이 그냥 안 온 경우)
    @Transactional
    public CompanionApplicationDto.Response markNoShow(UUID hostId, UUID applicationId) {
        CompanionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));
        verifyHost(hostId, application.getCompanion());
        if (!CompanionApplication.STATUS_APPROVED.equals(application.getStatus())) {
            throw new IllegalArgumentException("승인된 참여자만 노쇼 처리할 수 있습니다.");
        }

        application.markNoShow();
        application.getApplicant().recordNoShow();
        // 노쇼 확정 — 결제된 보증금 몰수 (Phase 7)
        paymentService.forfeitDepositOnNoShow(application.getCompanion(), application.getApplicant());
        return CompanionApplicationDto.Response.from(application);
    }

    public List<CompanionDto.Response> getMyJoined(UUID applicantId) {
        return applicationRepository
                .findByApplicant_IdAndStatusOrderByCreatedAtDesc(applicantId, CompanionApplication.STATUS_APPROVED)
                .stream()
                .map(app -> CompanionDto.Response.from(app.getCompanion(), currentApprovedCount(app.getCompanion().getCompanionId())))
                .toList();
    }

    // ==================== 내부 유틸 ====================

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private void requirePhoneVerified(User user) {
        if (!user.isPhoneVerified()) {
            throw new IllegalArgumentException("본인 인증이 필요한 기능입니다.");
        }
    }

    private void requireNotSanctioned(User user) {
        if (!user.isSanctioned()) {
            return;
        }
        if (User.SANCTION_BANNED.equals(user.getSanctionLevel())) {
            throw new IllegalArgumentException("반복적인 노쇼로 인해 동행 기능 이용이 영구 제한되었습니다.");
        }
        throw new IllegalArgumentException(
                "반복적인 노쇼로 인해 " + user.getRestrictedUntil().toLocalDate() + "까지 동행 기능 이용이 제한됩니다.");
    }

    // 여행 시작 48시간 전 시점을 지났는지(=임박 취소 or 이미 시작 후 취소인지) 판단
    private boolean isWithinNoShowWindow(Companion companion) {
        if (companion.getStartDate() == null) {
            return false;
        }
        LocalDateTime deadline = companion.getStartDate().atStartOfDay().minusHours(NO_SHOW_WINDOW_HOURS);
        return !LocalDateTime.now().isBefore(deadline);
    }

    private Companion findCompanionAsHost(UUID hostId, UUID companionId) {
        Companion companion = companionRepository.findById(companionId)
                .orElseThrow(() -> new IllegalArgumentException("모집글을 찾을 수 없습니다."));
        verifyHost(hostId, companion);
        return companion;
    }

    private void verifyHost(UUID hostId, Companion companion) {
        if (!companion.getHost().getId().equals(hostId)) {
            throw new IllegalArgumentException("방장만 수행할 수 있는 작업입니다.");
        }
    }

    private long currentApprovedCount(UUID companionId) {
        return applicationRepository.countByCompanion_CompanionIdAndStatus(companionId, CompanionApplication.STATUS_APPROVED);
    }

    private void notifyConfirmedParticipants(Companion companion, String message) {
        applicationRepository.findByCompanion_CompanionIdOrderByCreatedAtDesc(companion.getCompanionId()).stream()
                .filter(app -> CompanionApplication.STATUS_APPROVED.equals(app.getStatus()))
                .forEach(app -> fcmService.sendNotification(app.getApplicant().getId(), companion.getTitle(), message));
    }
}
