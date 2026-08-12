package com.capstone.travelbusan.domain.guide.service;

import com.capstone.travelbusan.domain.guide.dto.GuideScheduleDto;
import com.capstone.travelbusan.domain.guide.dto.request.GuideProductRequestDto;
import com.capstone.travelbusan.domain.guide.dto.response.GuideProductResponseDto;
import com.capstone.travelbusan.domain.guide.entity.GuideProduct;
import com.capstone.travelbusan.domain.guide.entity.GuideServiceDetail;
import com.capstone.travelbusan.domain.guide.repository.GuideProductRepository;
import com.capstone.travelbusan.domain.guide.repository.GuideServiceDetailRepository;
import com.capstone.travelbusan.domain.guider.entity.GuiderInfo;
import com.capstone.travelbusan.domain.guider.repository.GuiderInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuideProductService {

    private final GuideProductRepository guideProductRepository;
    private final GuiderInfoRepository guiderInfoRepository;
    private final GuideServiceDetailRepository serviceDetailRepository;

    // ==================== 사용자용 ====================

    // 게시된 상품만 반환 (사용자 가이드 탐색 화면)
    public List<GuideProductResponseDto> getAllPublishedProducts() {
        return guideProductRepository.findByIsPublishedTrue().stream()
                .map(GuideProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // ==================== 가이드용 ====================

    // 내 상품 전체 반환 (미게시 포함, 상품 관리 화면)
    public List<GuideProductResponseDto> getMyProducts(UUID userId) {
        return guideProductRepository.findByGuider_GuideId(userId).stream()
                .map(GuideProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 내 게시된 상품만 반환 (포트폴리오 화면)
    public List<GuideProductResponseDto> getMyPublishedProducts(UUID userId) {
        return guideProductRepository.findByGuider_GuideIdAndIsPublishedTrue(userId).stream()
                .map(GuideProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 상품 등록
    @Transactional
    public GuideProductResponseDto createProduct(UUID userId, GuideProductRequestDto request) {
        GuiderInfo guider = findGuider(userId);

        GuideProduct product = GuideProduct.builder()
                .guider(guider)
                .title(request.getTitle())
                .description(request.getDescription())
                .region(request.getRegion())
                .durationMinutes(request.getDurationMinutes())
                .maxCapacity(request.getMaxCapacity())
                .pricePerPerson(request.getPricePerPerson())
                .hasCar(request.getHasCar() != null && request.getHasCar())
                .availableLanguages(request.getAvailableLanguages())
                .meetingPoint(request.getMeetingPoint())
                .meetingPointDesc(request.getMeetingPointDesc())
                .includedItems(request.getIncludedItems())
                .excludedItems(request.getExcludedItems())
                .relatedMaterials(request.getRelatedMaterials())
                .build();

        GuideProduct saved = guideProductRepository.save(product);
        saveSchedules(saved, request.getSchedules());

        return GuideProductResponseDto.from(saved);
    }

    // 상품 수정
    @Transactional
    public GuideProductResponseDto updateProduct(UUID userId, UUID serviceId, GuideProductRequestDto request) {
        GuideProduct product = findMyProduct(userId, serviceId);

        GuideProduct updated = GuideProduct.builder()
                .serviceId(product.getServiceId())
                .guider(product.getGuider())
                .title(request.getTitle())
                .description(request.getDescription())
                .region(request.getRegion())
                .durationMinutes(request.getDurationMinutes())
                .maxCapacity(request.getMaxCapacity())
                .pricePerPerson(request.getPricePerPerson())
                .hasCar(request.getHasCar() != null && request.getHasCar())
                .availableLanguages(request.getAvailableLanguages())
                .meetingPoint(request.getMeetingPoint())
                .meetingPointDesc(request.getMeetingPointDesc())
                .includedItems(request.getIncludedItems())
                .excludedItems(request.getExcludedItems())
                .relatedMaterials(request.getRelatedMaterials())
                .isPublished(product.getIsPublished()) // 게시 상태 유지
                .build();

        GuideProduct saved = guideProductRepository.save(updated);

        // 기존 스케줄 삭제 후 새로 저장
        serviceDetailRepository.deleteByService_ServiceId(serviceId);
        saveSchedules(saved, request.getSchedules());

        return GuideProductResponseDto.from(saved);
    }

    // 상품 삭제
    @Transactional
    public void deleteProduct(UUID userId, UUID serviceId) {
        GuideProduct product = findMyProduct(userId, serviceId);
        guideProductRepository.delete(product);
    }

    // 게시 / 게시취소 토글
    @Transactional
    public GuideProductResponseDto togglePublish(UUID userId, UUID serviceId) {
        GuideProduct product = findMyProduct(userId, serviceId);

        GuideProduct toggled = GuideProduct.builder()
                .serviceId(product.getServiceId())
                .guider(product.getGuider())
                .title(product.getTitle())
                .description(product.getDescription())
                .region(product.getRegion())
                .durationMinutes(product.getDurationMinutes())
                .maxCapacity(product.getMaxCapacity())
                .pricePerPerson(product.getPricePerPerson())
                .hasCar(product.getHasCar())
                .availableLanguages(product.getAvailableLanguages())
                .meetingPoint(product.getMeetingPoint())
                .meetingPointDesc(product.getMeetingPointDesc())
                .includedItems(product.getIncludedItems())
                .excludedItems(product.getExcludedItems())
                .relatedMaterials(product.getRelatedMaterials())
                .isPublished(!product.getIsPublished()) // 게시 상태 반전
                .build();

        return GuideProductResponseDto.from(guideProductRepository.save(toggled));
    }

    // ==================== 내부 헬퍼 ====================

    private GuiderInfo findGuider(UUID userId) {
        return guiderInfoRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("가이더 정보를 찾을 수 없습니다."));
    }

    private GuideProduct findMyProduct(UUID userId, UUID serviceId) {
        GuideProduct product = guideProductRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        if (!product.getGuider().getGuideId().equals(userId)) {
            throw new IllegalArgumentException("본인의 상품만 수정/삭제할 수 있습니다.");
        }
        return product;
    }

    private void saveSchedules(GuideProduct product, List<GuideScheduleDto> schedules) {
        if (schedules == null) return;
        for (int i = 0; i < schedules.size(); i++) {
            GuideScheduleDto s = schedules.get(i);
            serviceDetailRepository.save(
                    GuideServiceDetail.builder()
                            .service(product)
                            .sequenceOrder(i + 1)
                            .startTime(LocalTime.parse(s.getStartTime()))
                            .endTime(LocalTime.parse(s.getEndTime()))
                            .location(s.getTitle())
                            .content(s.getDescription())
                            .build()
            );
        }
    }
}