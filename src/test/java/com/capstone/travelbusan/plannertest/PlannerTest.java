package com.capstone.travelbusan.plannertest;

import com.capstone.travelbusan.domain.planner.controller.PlannerController;
import com.capstone.travelbusan.domain.planner.dto.ItineraryDetailResponseDto;
import com.capstone.travelbusan.domain.planner.dto.ItineraryResponseDto;
import com.capstone.travelbusan.domain.planner.service.PlannerService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlannerController.class)
@MockBean(JpaMetamodelMappingContext.class)
class PlannerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlannerService plannerService;

    @Test
    @DisplayName("내 일정 단건 조회 성공 테스트")
    void getItinerary_Success() throws Exception {
        // --- [Given] 가짜 데이터 및 인증 정보 설정 ---
        Long itineraryId = 1L;
        UUID mockUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        // UserPrincipal.create(String userId, String email, boolean isGuide) 형식에 맞춤
        UserPrincipal mockPrincipal = UserPrincipal.create(
                mockUserId.toString(),
                "user@test.com",
                false
        );

        ItineraryDetailResponseDto mockDetail = ItineraryDetailResponseDto.builder()
                .detailId(101L)
                .dayNumber(1)
                .startTime(LocalTime.of(10, 0))
                .durationMinutes(90)
                .placeName("달맞이동산")
                .latitude(35.1571305311)
                .longitude(129.1821532774)
                .build();

        ItineraryResponseDto mockResponse = ItineraryResponseDto.builder()
                .itineraryId(itineraryId)
                .title("부모님과 함께하는 1박 2일 힐링 여행")
                .region("부산광역시")
                .startDate(LocalDate.of(2026, 4, 6))
                .endDate(LocalDate.of(2026, 4, 7))
                .details(List.of(mockDetail))
                .build();

        given(plannerService.getMyItinerary(eq(itineraryId), any(UUID.class)))
                .willReturn(mockResponse);

        // --- [When & Then] API 호출 및 검증 ---
        mockMvc.perform(get("/api/v1/planner/itineraries/{itinerary_id}", itineraryId)
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itineraryId").value(itineraryId))
                .andExpect(jsonPath("$.title").value("부모님과 함께하는 1박 2일 힐링 여행"))
                .andExpect(jsonPath("$.details[0].placeName").value("달맞이동산"))
                .andDo(print());
    }
}