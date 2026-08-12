package com.capstone.travelbusan.companiontest;

import com.capstone.travelbusan.domain.companion.dto.CompanionApplicationDto;
import com.capstone.travelbusan.domain.companion.dto.CompanionDto;
import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.companion.entity.CompanionApplication;
import com.capstone.travelbusan.domain.companion.repository.CompanionApplicationRepository;
import com.capstone.travelbusan.domain.companion.repository.CompanionRepository;
import com.capstone.travelbusan.domain.companion.service.CompanionService;
import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.planner.repository.ItineraryRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 동행(companion) 도메인 핵심 비즈니스 로직 테스트.
 * DB 없이 Mockito로 리포지토리를 흉내내서 검증하므로, DB 연결 없이도 바로 실행 가능하다.
 * (guider_reg_test/GuiderRegistrationServiceTest 스타일을 그대로 따랐다.)
 */
@ExtendWith(MockitoExtension.class)
class CompanionServiceTest {

    @Mock private CompanionRepository companionRepository;
    @Mock private CompanionApplicationRepository applicationRepository;
    @Mock private ItineraryRepository itineraryRepository;
    @Mock private UserRepository userRepository;
    @Mock private FcmService fcmService;

    @InjectMocks
    private CompanionService companionService;

    // ==================== 동행 모집하기 ====================

    @Test
    @DisplayName("모집글 생성 성공: 본인 인증된 방장이 본인 일정으로 모집글을 만들면 RECRUITING 상태로 저장된다")
    void createCompanion_Success() {
        // given
        UUID hostId = UUID.randomUUID();
        User host = mock(User.class);
        given(host.getId()).willReturn(hostId);
        given(host.isPhoneVerified()).willReturn(true);
        given(host.getNickname()).willReturn("방장닉네임");

        Itinerary itinerary = mock(Itinerary.class);
        given(itinerary.getItineraryId()).willReturn(1L);
        given(itinerary.getUserId()).willReturn(hostId);
        given(itinerary.getStartDate()).willReturn(LocalDate.of(2026, 9, 1));
        given(itinerary.getEndDate()).willReturn(LocalDate.of(2026, 9, 2));
        given(itinerary.getTitle()).willReturn("해운대 여행");
        given(itinerary.getRegion()).willReturn("부산 해운대구");

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(itineraryRepository.findById(1L)).willReturn(Optional.of(itinerary));
        given(companionRepository.save(any(Companion.class))).willAnswer(inv -> inv.getArgument(0));

        CompanionDto.CreateRequest request = createRequest(1L, 2, 4);

        // when
        CompanionDto.Response response = companionService.createCompanion(hostId, request);

        // then
        assertThat(response.getStatus()).isEqualTo(Companion.STATUS_RECRUITING);
        assertThat(response.getApprovedCount()).isZero();
        verify(companionRepository, times(1)).save(any(Companion.class));
    }

    @Test
    @DisplayName("모집글 생성 실패: 본인 인증이 안 된 사용자는 모집글을 만들 수 없다")
    void createCompanion_Fail_NotPhoneVerified() {
        // given
        UUID hostId = UUID.randomUUID();
        User host = mock(User.class);
        given(host.isPhoneVerified()).willReturn(false);
        given(userRepository.findById(hostId)).willReturn(Optional.of(host));

        // when & then
        assertThatThrownBy(() -> companionService.createCompanion(hostId, createRequest(1L, 2, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인 인증");

        verify(itineraryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("모집글 생성 실패: 타인의 일정으로는 모집글을 만들 수 없다")
    void createCompanion_Fail_NotOwnItinerary() {
        // given
        UUID hostId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User host = mock(User.class);
        given(host.isPhoneVerified()).willReturn(true);
        given(userRepository.findById(hostId)).willReturn(Optional.of(host));

        Itinerary itinerary = mock(Itinerary.class);
        given(itinerary.getUserId()).willReturn(otherUserId); // 방장의 일정이 아님
        given(itineraryRepository.findById(1L)).willReturn(Optional.of(itinerary));

        // when & then
        assertThatThrownBy(() -> companionService.createCompanion(hostId, createRequest(1L, 2, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 일정만");
    }

    // ==================== 참여 신청 ====================

    @Test
    @DisplayName("참여 신청 성공: 본인 인증된 참여자가 모집중인 동행에 신청하면 PENDING 상태로 저장된다")
    void apply_Success() {
        // given
        UUID applicantId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        User applicant = mock(User.class);
        given(applicant.getId()).willReturn(applicantId);
        given(applicant.isPhoneVerified()).willReturn(true);
        given(applicant.getNickname()).willReturn("참여자닉네임");

        User host = mock(User.class);
        given(host.getId()).willReturn(hostId);

        Companion companion = Companion.builder()
                .companionId(companionId)
                .host(host)
                .itinerary(mock(Itinerary.class))
                .minParticipants(2)
                .maxParticipants(4)
                .title("동행 모집")
                .build(); // 기본 상태 = RECRUITING

        given(userRepository.findById(applicantId)).willReturn(Optional.of(applicant));
        given(companionRepository.findById(companionId)).willReturn(Optional.of(companion));
        given(applicationRepository.existsByCompanion_CompanionIdAndApplicant_IdAndStatusIn(
                eq(companionId), eq(applicantId), anyList())).willReturn(false);
        given(applicationRepository.save(any(CompanionApplication.class))).willAnswer(inv -> inv.getArgument(0));

        CompanionApplicationDto.CreateRequest request = new CompanionApplicationDto.CreateRequest();
        ReflectionTestUtils.setField(request, "introduction", "같이 여행하고 싶어요!");

        // when
        CompanionApplicationDto.Response response = companionService.apply(applicantId, companionId, request);

        // then
        assertThat(response.getStatus()).isEqualTo(CompanionApplication.STATUS_PENDING);
        verify(fcmService, times(1)).sendNotification(eq(hostId), anyString(), anyString());
    }

    @Test
    @DisplayName("참여 신청 실패: 방장 본인은 자신이 만든 동행에 신청할 수 없다")
    void apply_Fail_HostCannotApplyToOwnCompanion() {
        // given
        UUID hostId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        User host = mock(User.class);
        given(host.getId()).willReturn(hostId);
        given(host.isPhoneVerified()).willReturn(true);

        Companion companion = Companion.builder()
                .companionId(companionId)
                .host(host)
                .itinerary(mock(Itinerary.class))
                .minParticipants(2)
                .maxParticipants(4)
                .title("동행 모집")
                .build();

        given(userRepository.findById(hostId)).willReturn(Optional.of(host));
        given(companionRepository.findById(companionId)).willReturn(Optional.of(companion));

        // when & then
        assertThatThrownBy(() -> companionService.apply(hostId, companionId, new CompanionApplicationDto.CreateRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인이 개설한 동행");
    }

    @Test
    @DisplayName("참여 신청 실패: 이미 대기중이거나 승인된 신청이 있으면 중복 신청할 수 없다")
    void apply_Fail_AlreadyApplied() {
        // given
        UUID applicantId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        User applicant = mock(User.class);
        given(applicant.isPhoneVerified()).willReturn(true);

        User host = mock(User.class);
        given(host.getId()).willReturn(UUID.randomUUID());

        Companion companion = Companion.builder()
                .companionId(companionId)
                .host(host)
                .itinerary(mock(Itinerary.class))
                .minParticipants(2)
                .maxParticipants(4)
                .title("동행 모집")
                .build();

        given(userRepository.findById(applicantId)).willReturn(Optional.of(applicant));
        given(companionRepository.findById(companionId)).willReturn(Optional.of(companion));
        given(applicationRepository.existsByCompanion_CompanionIdAndApplicant_IdAndStatusIn(
                eq(companionId), eq(applicantId), anyList())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> companionService.apply(applicantId, companionId, new CompanionApplicationDto.CreateRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 신청한");
    }

    @Test
    @DisplayName("참여 신청 실패: 이미 마감된 모집글에는 신청할 수 없다")
    void apply_Fail_NotRecruiting() {
        // given
        UUID applicantId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        User applicant = mock(User.class);
        given(applicant.isPhoneVerified()).willReturn(true);

        User host = mock(User.class);
        given(host.getId()).willReturn(UUID.randomUUID());

        Companion companion = Companion.builder()
                .companionId(companionId)
                .host(host)
                .itinerary(mock(Itinerary.class))
                .minParticipants(2)
                .maxParticipants(4)
                .title("동행 모집")
                .status(Companion.STATUS_CONFIRMED) // 이미 확정됨
                .build();

        given(userRepository.findById(applicantId)).willReturn(Optional.of(applicant));
        given(companionRepository.findById(companionId)).willReturn(Optional.of(companion));

        // when & then
        assertThatThrownBy(() -> companionService.apply(applicantId, companionId, new CompanionApplicationDto.CreateRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 마감된");
    }

    // ==================== 신청 승인 ====================

    @Test
    @DisplayName("신청 승인 실패: 승인 인원이 이미 최대 정원에 도달하면 더 승인할 수 없다")
    void approveApplication_Fail_CapacityFull() {
        // given
        UUID hostId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        User host = mock(User.class);
        given(host.getId()).willReturn(hostId);

        Companion companion = Companion.builder()
                .companionId(companionId)
                .host(host)
                .itinerary(mock(Itinerary.class))
                .minParticipants(1)
                .maxParticipants(2) // 정원 2명
                .title("동행 모집")
                .build();

        CompanionApplication application = CompanionApplication.builder()
                .companion(companion)
                .applicant(mock(User.class))
                .build();

        given(applicationRepository.findById(applicationId)).willReturn(Optional.of(application));
        given(applicationRepository.countByCompanion_CompanionIdAndStatus(companionId, CompanionApplication.STATUS_APPROVED))
                .willReturn(2L); // 이미 정원만큼 승인됨

        // when & then
        assertThatThrownBy(() -> companionService.approveApplication(hostId, applicationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("정원");
    }

    // ==================== 마감 / 최소 인원 미달 처리 ====================

    @Test
    @DisplayName("모집 마감: 승인 인원이 최소 인원 이상이면 CONFIRMED 상태로 전환된다")
    void closeForApplications_ConfirmedWhenMinimumMet() {
        // given
        UUID hostId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        User host = mock(User.class);
        given(host.getId()).willReturn(hostId);

        Companion companion = Companion.builder()
                .companionId(companionId)
                .host(host)
                .itinerary(mock(Itinerary.class))
                .minParticipants(2)
                .maxParticipants(4)
                .title("동행 모집")
                .build();

        given(companionRepository.findById(companionId)).willReturn(Optional.of(companion));
        given(applicationRepository.countByCompanion_CompanionIdAndStatus(companionId, CompanionApplication.STATUS_APPROVED))
                .willReturn(2L); // 최소 인원 충족
        given(applicationRepository.findByCompanion_CompanionIdOrderByCreatedAtDesc(companionId))
                .willReturn(List.of());

        // when
        CompanionDto.Response response = companionService.closeForApplications(hostId, companionId);

        // then
        assertThat(response.getStatus()).isEqualTo(Companion.STATUS_CONFIRMED);
    }

    @Test
    @DisplayName("모집 마감: 승인 인원이 최소 인원 미달이면 UNDER_MINIMUM 상태가 된다")
    void closeForApplications_UnderMinimumWhenNotMet() {
        // given
        UUID hostId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        User host = mock(User.class);
        given(host.getId()).willReturn(hostId);

        Companion companion = Companion.builder()
                .companionId(companionId)
                .host(host)
                .itinerary(mock(Itinerary.class))
                .minParticipants(3)
                .maxParticipants(4)
                .title("동행 모집")
                .build();

        given(companionRepository.findById(companionId)).willReturn(Optional.of(companion));
        given(applicationRepository.countByCompanion_CompanionIdAndStatus(companionId, CompanionApplication.STATUS_APPROVED))
                .willReturn(1L); // 최소 3명인데 1명뿐

        // when
        CompanionDto.Response response = companionService.closeForApplications(hostId, companionId);

        // then
        assertThat(response.getStatus()).isEqualTo(Companion.STATUS_UNDER_MINIMUM);
    }

    @Test
    @DisplayName("최소 인원 미달 결정: CONTINUE를 선택하면 소규모로 CONFIRMED 상태가 된다")
    void applyUnderMinimumDecision_Continue() {
        assertDecisionResult("CONTINUE", Companion.STATUS_CONFIRMED);
    }

    @Test
    @DisplayName("최소 인원 미달 결정: CANCEL을 선택하면 CANCELED 상태가 된다")
    void applyUnderMinimumDecision_Cancel() {
        assertDecisionResult("CANCEL", Companion.STATUS_CANCELED);
    }

    @Test
    @DisplayName("최소 인원 미달 결정: POSTPONE을 선택하면 다시 RECRUITING 상태로 돌아간다")
    void applyUnderMinimumDecision_Postpone() {
        assertDecisionResult("POSTPONE", Companion.STATUS_RECRUITING);
    }

    private void assertDecisionResult(String decision, String expectedStatus) {
        // given
        UUID hostId = UUID.randomUUID();
        UUID companionId = UUID.randomUUID();

        User host = mock(User.class);
        given(host.getId()).willReturn(hostId);

        Companion companion = Companion.builder()
                .companionId(companionId)
                .host(host)
                .itinerary(mock(Itinerary.class))
                .minParticipants(3)
                .maxParticipants(4)
                .title("동행 모집")
                .status(Companion.STATUS_UNDER_MINIMUM)
                .build();

        given(companionRepository.findById(companionId)).willReturn(Optional.of(companion));
        given(applicationRepository.countByCompanion_CompanionIdAndStatus(companionId, CompanionApplication.STATUS_APPROVED))
                .willReturn(1L);
        given(applicationRepository.findByCompanion_CompanionIdOrderByCreatedAtDesc(companionId))
                .willReturn(List.of());

        CompanionDto.DecisionRequest request = new CompanionDto.DecisionRequest();
        ReflectionTestUtils.setField(request, "decision", decision);

        // when
        CompanionDto.Response response = companionService.applyUnderMinimumDecision(hostId, companionId, request);

        // then
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
    }

    // ==================== 도움 메서드 ====================

    private CompanionDto.CreateRequest createRequest(Long itineraryId, int min, int max) {
        CompanionDto.CreateRequest request = new CompanionDto.CreateRequest();
        ReflectionTestUtils.setField(request, "itineraryId", itineraryId);
        ReflectionTestUtils.setField(request, "title", "테스트 모집글");
        ReflectionTestUtils.setField(request, "minParticipants", min);
        ReflectionTestUtils.setField(request, "maxParticipants", max);
        ReflectionTestUtils.setField(request, "preferenceTags", List.of("자연", "사진"));
        ReflectionTestUtils.setField(request, "costSharingNote", "각자 부담");
        ReflectionTestUtils.setField(request, "description", "같이 다녀요");
        return request;
    }
}
