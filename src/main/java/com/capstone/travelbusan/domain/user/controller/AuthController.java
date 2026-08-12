package com.capstone.travelbusan.domain.user.controller;

import com.capstone.travelbusan.domain.user.dto.AuthDto;
import com.capstone.travelbusan.domain.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth") //
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login") //
    public ResponseEntity<AuthDto.LoginSuccessResponse> login(@RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthData authData = authService.login(request);

        return ResponseEntity.ok(
                new AuthDto.LoginSuccessResponse("success", authData)
        );
    }
    //회원가입 요청
    @PostMapping("/signup")
    public ResponseEntity<AuthDto.SignUpResponse> signUp(@RequestBody AuthDto.SignUpRequest request) {
        AuthDto.SignUpResponse response = authService.signUp(request);
        return ResponseEntity.ok(response);
    }
}