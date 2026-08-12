package com.capstone.travelbusan.domain.guider.controller;

import com.capstone.travelbusan.domain.guider.dto.request.GuiderRegisterRequestDto;
import com.capstone.travelbusan.domain.guider.dto.response.GuiderRegisterResponseDto;
import com.capstone.travelbusan.domain.guider.service.GuiderRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guider")
@RequiredArgsConstructor
public class GuiderController {

    private final GuiderRegistrationService guiderRegistrationService;

    @PostMapping("/{user_id}/register")
    public ResponseEntity<GuiderRegisterResponseDto> registerAsGuider(
            @PathVariable("user_id") UUID userId,
            @RequestBody GuiderRegisterRequestDto requestDto) {

        GuiderRegisterResponseDto response = guiderRegistrationService.registerGuider(userId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}