package com.capstone.travelbusan.domain.bidapplication.service;

import com.capstone.travelbusan.domain.bidapplication.dto.BidApplicationDto;
import com.capstone.travelbusan.domain.bidapplication.entity.BidApplication;
import com.capstone.travelbusan.domain.bidapplication.repository.BidApplicationRepository;
import com.capstone.travelbusan.domain.chat.dto.ChatDto;
import com.capstone.travelbusan.domain.chat.service.ChatService;
import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import com.capstone.travelbusan.domain.userbid.entity.UserBid;
import com.capstone.travelbusan.domain.userbid.repository.UserBidRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BidApplicationService {

    private final BidApplicationRepository bidApplicationRepository;
    private final UserBidRepository userBidRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final FcmService fcmService;

    // 가이드 입찰 참여
    @Transactional
    public BidApplicationDto.Response apply(UUID guideId, UUID bidId) {
        // 중복 참여 체크
        if (bidApplicationRepository.existsByUserBid_BidIdAndGuide_Id(bidId, guideId)) {
            throw new IllegalArgumentException("이미 참여한 입찰입니다.");
        }
        UserBid bid = userBidRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));
        if (bid.getIsClosed()) {
            throw new IllegalArgumentException("이미 마감된 입찰입니다.");
        }
        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        BidApplication application = BidApplication.builder()
                .userBid(bid)
                .guide(guide)
                .build();

        BidApplication saved = bidApplicationRepository.save(application);

        // 사용자에게 알림
        fcmService.sendNotification(
                bid.getUser().getId(),
                "가이드 입찰 참여",
                guide.getNickname() + " 가이드가 입찰에 참여했습니다."
        );

        return BidApplicationDto.Response.from(saved);
    }

    // 가이드 참여 취소
    @Transactional
    public void cancel(UUID guideId, UUID bidId) {
        BidApplication application = bidApplicationRepository
                .findByUserBid_BidIdAndGuide_Id(bidId, guideId)
                .orElseThrow(() -> new IllegalArgumentException("참여 내역을 찾을 수 없습니다."));
        bidApplicationRepository.delete(application);
    }

    // 특정 입찰 참여 가이드 목록 조회
    public List<BidApplicationDto.Response> getApplications(UUID bidId) {
        return bidApplicationRepository.findByUserBid_BidId(bidId).stream()
                .map(BidApplicationDto.Response::from)
                .collect(Collectors.toList());
    }

    // 사용자가 가이드 선택 → 채팅방 생성 + 입찰 마감
    @Transactional
    public ChatDto.RoomResponse selectGuide(UUID userId, UUID applicationId) {
        BidApplication application = bidApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("참여 내역을 찾을 수 없습니다."));

        UserBid bid = application.getUserBid();

        // 본인 요청인지 확인
        if (!bid.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 요청만 가이드를 선택할 수 있습니다.");
        }

        // 채팅방 생성
        ChatDto.RoomResponse room = chatService.createRoom(
                bid.getBidId(),
                userId,
                application.getGuide().getId()
        );
        bid.close();
        userBidRepository.save(bid);
        // 선택된 가이드에게 알림
        fcmService.sendNotification(
                application.getGuide().getId(),
                "가이드 선택됨",
                bid.getUser().getNickname() + "님이 당신을 가이드로 선택했습니다."
        );

        return room;
    }
}