package com.moa.web.chat;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moa.common.exception.ApiResponse;
import com.moa.dto.chat.request.ChatRequest;
import com.moa.dto.chat.response.ChatResponse;
import com.moa.service.chat.ChatBotService;

@RestController
@RequestMapping("/api/chatbot")
public class ChatBotRestController {

	private final ChatBotService chatBotService;

	public ChatBotRestController(ChatBotService chatBotService) {
		this.chatBotService = chatBotService;
	}

	@PostMapping("/message")
	public ApiResponse<ChatResponse> message(@Validated @RequestBody ChatRequest req) {
		ChatResponse reply = chatBotService.chat(req);
		return ApiResponse.success(reply);
	}
}
