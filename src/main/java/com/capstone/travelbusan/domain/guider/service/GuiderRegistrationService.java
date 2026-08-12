package com.capstone.travelbusan.domain.guider.service;

import com.capstone.travelbusan.domain.guider.dto.request.GuiderRegisterRequestDto;
import com.capstone.travelbusan.domain.guider.dto.response.GuiderRegisterResponseDto;
import com.capstone.travelbusan.domain.guider.entity.GuiderInfo;
import com.capstone.travelbusan.domain.guider.repository.GuiderInfoRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuiderRegistrationService {

    private final UserRepository userRepository;
    private final GuiderInfoRepository guiderInfoRepository;

    @Transactional
    public GuiderRegisterResponseDto registerGuider(UUID userId, GuiderRegisterRequestDto request) {

        // 1. 유저 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. ID: " + userId));

        // 2. 이미 가이드로 등록된 유저인지 방어 로직
        if (user.isGuide() || guiderInfoRepository.existsById(userId)) {
            throw new IllegalStateException("이미 가이드로 등록된 사용자입니다.");
        }

        // 3. 가이드 프로필(GuiderInfo) 엔티티 생성
        GuiderInfo guiderInfo = GuiderInfo.builder()
                .user(user) // @MapsId에 의해 user_id가 guide_id로 들어감
                .activeRegions(request.getActiveRegions())
                .availableLanguages(request.getAvailableLanguages())
                .experiencePeriod(request.getExperiencePeriod())
                .languageScores(request.getLanguageScores())
                .introduction(request.getIntroduction())
                .specialties(request.getSpecialties())
                .build();

        // 4. 프로필 저장
        guiderInfoRepository.save(guiderInfo);

        // 5. 유저 테이블의 is_guide 플래그를 true로 업데이트 (JPA 더티 체킹)
        user.promoteToGuide();

        return GuiderRegisterResponseDto.of(user.getId());
    }
}