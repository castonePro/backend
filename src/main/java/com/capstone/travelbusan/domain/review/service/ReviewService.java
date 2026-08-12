package com.capstone.travelbusan.domain.review.service;

import com.capstone.travelbusan.domain.guide.entity.GuideProduct;
import com.capstone.travelbusan.domain.guide.repository.GuideProductRepository;
import com.capstone.travelbusan.domain.review.dto.ReviewDto;
import com.capstone.travelbusan.domain.review.entity.Review;
import com.capstone.travelbusan.domain.review.repository.ReviewRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GuideProductRepository guideProductRepository;

    // 리뷰 작성
    @Transactional
    public ReviewDto.Response createReview(UUID userId, ReviewDto.Request request) {
        // 중복 리뷰 방지
        if (request.getServiceId() != null &&
                reviewRepository.existsByUser_IdAndService_ServiceId(userId, request.getServiceId())) {
            throw new IllegalArgumentException("이미 리뷰를 작성한 상품입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        User guide = userRepository.findById(request.getGuideId())
                .orElseThrow(() -> new IllegalArgumentException("가이드를 찾을 수 없습니다."));

        GuideProduct service = null;
        if (request.getServiceId() != null) {
            service = guideProductRepository.findById(request.getServiceId()).orElse(null);
        }

        Review review = Review.builder()
                .guide(guide)
                .user(user)
                .service(service)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        return ReviewDto.Response.from(reviewRepository.save(review));
    }

    // 가이드 리뷰 목록
    public List<ReviewDto.Response> getGuideReviews(UUID guideId) {
        return reviewRepository.findByGuide_IdOrderByCreatedAtDesc(guideId).stream()
                .map(ReviewDto.Response::from)
                .collect(Collectors.toList());
    }

    // 가이드 평점/리뷰 수 요약
    public ReviewDto.Summary getGuideSummary(UUID guideId) {
        Double avg = reviewRepository.findAverageRatingByGuideId(guideId);
        long count = reviewRepository.countByGuide_Id(guideId);

        return ReviewDto.Summary.builder()
                .guideId(guideId)
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .reviewCount(count)
                .build();
    }
}