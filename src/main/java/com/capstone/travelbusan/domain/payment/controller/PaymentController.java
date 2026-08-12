package com.capstone.travelbusan.domain.payment.controller;

import com.capstone.travelbusan.domain.payment.dto.PaymentDto;
import com.capstone.travelbusan.domain.payment.service.PaymentService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 내 결제 내역 (수수료·보증금·부스트 전체)
    @GetMapping("/my")
    public ResponseEntity<List<PaymentDto.Response>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(paymentService.getMyPayments(currentUser.getUserId()));
    }

    // 대기중인 결제 실행 (Mock PG — 즉시 승인)
    @PostMapping("/{paymentId}/pay")
    public ResponseEntity<PaymentDto.Response> pay(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.pay(currentUser.getUserId(), paymentId));
    }
}
