package com.capstone.travelbusan.domain.report.controller;

import com.capstone.travelbusan.domain.report.dto.ReportDto;
import com.capstone.travelbusan.domain.report.service.ReportService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 신고 접수
    @PostMapping
    public ResponseEntity<ReportDto.Response> submit(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody ReportDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.submitReport(currentUser.getUserId(), request));
    }

    // 내가 접수한 신고 목록
    @GetMapping("/my")
    public ResponseEntity<List<ReportDto.Response>> getMyReports(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(reportService.getMyReports(currentUser.getUserId()));
    }
}
