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
        // platform이 "web"이면 웹 전용 테이블에 저장한다 (앱 토큰을 덮어쓰지 않도록).
        // Flutter 앱은 platform 없이 보내므로 기존 동작 그대로다.
        if ("web".equalsIgnoreCase(request.getPlatform())) {
            fcmService.saveWebToken(currentUser.getUserId(), request.getToken());
        } else {
            fcmService.saveToken(currentUser.getUserId(), request.getToken());
        }
        return ResponseEntity.ok().build();
    }

    /** 웹 로그아웃 시 해당 브라우저 토큰 삭제 */
    @DeleteMapping("/token")
    public ResponseEntity<Void> deleteWebToken(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody TokenRequest request) {
        fcmService.deleteWebToken(currentUser.getUserId(), request.getToken());
        return ResponseEntity.noContent().build();
    }

    @Getter
    public static class TokenRequest {
        private String token;
        /** "web"이면 웹 푸시 토큰. 없으면(앱) 기존 fcm_tokens에 저장 */
        private String platform;
    }
}