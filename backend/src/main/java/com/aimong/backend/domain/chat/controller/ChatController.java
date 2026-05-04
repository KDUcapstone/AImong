package com.aimong.backend.domain.chat.controller;

import com.aimong.backend.domain.chat.dto.ChatRequest;
import com.aimong.backend.domain.chat.dto.ChatResponse;
import com.aimong.backend.domain.chat.service.ChatService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ApiResponse<ChatResponse> send(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(chatService.send(
                UUID.fromString(authentication.getName()),
                request.message(),
                request.masked()
        ));
    }
}
