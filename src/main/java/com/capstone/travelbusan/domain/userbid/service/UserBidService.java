package com.capstone.travelbusan.domain.userbid.service;

import com.capstone.travelbusan.domain.userbid.dto.UserBidDto;
import com.capstone.travelbusan.domain.userbid.entity.UserBid;
import com.capstone.travelbusan.domain.userbid.repository.UserBidRepository;
import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.planner.repository.ItineraryRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserBidService {

    private final UserBidRepository userBidRepository;
    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;

    // 사용자: 역으로 제안하기
    @Transactional
    public UserBidDto.Response createUserBid(UUID userId, Long itineraryId) {
        // 중복 요청 방지
        if (userBidRepository.existsByItinerary_ItineraryIdAndUser_Id(itineraryId, userId)) {
            throw new IllegalArgumentException("이미 제안한 일정입니다.");
        }

        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        if (!itinerary.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 일정만 제안할 수 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UserBid userBid = UserBid.builder()
                .itinerary(itinerary)
                .user(user)
                .build();

        return UserBidDto.Response.from(userBidRepository.save(userBid));
    }

    // 가이드: 입찰 현황 전체 조회
    public List<UserBidDto.Response> getAllUserBids() {
        return userBidRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(UserBidDto.Response::from)
                .toList();
    }

    // 사용자: 본인 요청 목록 조회
    public List<UserBidDto.Response> getMyUserBids(UUID userId) {
        return userBidRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(UserBidDto.Response::from)
                .toList();
    }
}