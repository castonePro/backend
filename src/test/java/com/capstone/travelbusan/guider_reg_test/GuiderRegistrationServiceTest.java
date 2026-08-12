package com.capstone.travelbusan.guider_reg_test;

import com.capstone.travelbusan.domain.guider.dto.request.GuiderRegisterRequestDto;
import com.capstone.travelbusan.domain.guider.dto.request.LanguageScoreDto;
import com.capstone.travelbusan.domain.guider.dto.response.GuiderRegisterResponseDto;
import com.capstone.travelbusan.domain.guider.entity.GuiderInfo;
import com.capstone.travelbusan.domain.guider.repository.GuiderInfoRepository;
import com.capstone.travelbusan.domain.guider.service.GuiderRegistrationService;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuiderRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuiderInfoRepository guiderInfoRepository;

    @InjectMocks
    private GuiderRegistrationService guiderRegistrationService;

    // --- [성공 케이스] ---

    @Test
    @DisplayName("가이드 등록 성공: 모든 조건 충족 시 프로필이 저장되고 유저 상태가 변경된다")
    void registerGuider_Success() {
        // given
        UUID userId = UUID.randomUUID();
        User mockUser = mock(User.class);
        GuiderRegisterRequestDto request = createRequest();

        given(mockUser.getId()).willReturn(userId);
        given(mockUser.isGuide()).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(guiderInfoRepository.existsById(userId)).willReturn(false);

        // when
        GuiderRegisterResponseDto response = guiderRegistrationService.registerGuider(userId, request);

        // then
        assertThat(response.getGuideId()).isEqualTo(userId);
        verify(mockUser, times(1)).promoteToGuide(); // 유저 승격 메서드 호출 확인
        verify(guiderInfoRepository, times(1)).save(any(GuiderInfo.class)); // 프로필 저장 확인
    }

    // --- [실패 케이스] ---

    @Test
    @DisplayName("가이드 등록 실패: 존재하지 않는 유저 ID인 경우 IllegalArgumentException 발생")
    void registerGuider_Fail_UserNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> guiderRegistrationService.registerGuider(userId, createRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 유저");

        // 유저가 없으므로 다음 로직들이 실행되지 않았음을 검증
        verify(guiderInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("가이드 등록 실패: 이미 가이드 권한(isGuide=true)을 가진 유저인 경우 IllegalStateException 발생")
    void registerGuider_Fail_AlreadyGuideStatus() {
        // given
        UUID userId = UUID.randomUUID();
        User mockUser = mock(User.class);
        given(mockUser.isGuide()).willReturn(true); // 이미 가이드임
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        // when & then
        assertThatThrownBy(() -> guiderRegistrationService.registerGuider(userId, createRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 가이드로 등록된 사용자");
    }

    @Test
    @DisplayName("가이드 등록 실패: 유저 상태는 false지만 이미 가이드 프로필이 존재하는 경우 IllegalStateException 발생")
    void registerGuider_Fail_ProfileAlreadyExists() {
        // given
        UUID userId = UUID.randomUUID();
        User mockUser = mock(User.class);
        given(mockUser.isGuide()).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(guiderInfoRepository.existsById(userId)).willReturn(true); // 프로필이 이미 있음

        // when & then
        assertThatThrownBy(() -> guiderRegistrationService.registerGuider(userId, createRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 가이드로 등록된 사용자");
    }

    // --- [도움 메서드] ---

    private GuiderRegisterRequestDto createRequest() {
        return GuiderRegisterRequestDto.builder()
                .activeRegions(List.of("부산", "여수"))
                .availableLanguages(List.of("한국어", "영어"))
                .experiencePeriod("1~2년")
                .languageScores(List.of(new LanguageScoreDto("TOEIC", "900")))
                .introduction("반갑습니다. 부산 토박이 가이드입니다.")
                .specialties(List.of("맛집 투어", "야경 투어"))
                .build();
    }
}