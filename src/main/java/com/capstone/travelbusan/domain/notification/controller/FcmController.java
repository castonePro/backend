package com.capstone.travelbusan.domain.notification.controller;

import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @PostMapping("/token")
    public ResponseEntity<Void> saveToken(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody TokenRequest request) {
        fcmService.saveToken(currentUser.getUserId(), request.getToken());
        return ResponseEntity.ok().build();
    }

    @Getter
    public static class TokenRequest {
        private String token;
    }
}