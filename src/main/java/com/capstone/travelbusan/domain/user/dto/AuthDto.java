package com.capstone.travelbusan.domain.user.dto;

public class AuthDto {

    // 회원가입
    // Request to membership
    public record SignUpRequest(
            String email,
            String password,
            String nickname
    ) {}

    // Response to membership
    public record SignUpResponse(
            String status,
            String message
    ) {}

    //login

    // Request:
    public record LoginRequest(
            String email,
            String password
    ) {}

    // Response Data:
    public record AuthData(
            String user_id,
            String access_token,
            String nickname,
            boolean is_guide
    ) {}

    // Success Response:
    public record LoginSuccessResponse(
            String status,
            AuthData data
    ) {}

    // Fail Response:
    public record LoginFailResponse(
            String status,
            String message,
            int failed_attempts
    ) {}
}
