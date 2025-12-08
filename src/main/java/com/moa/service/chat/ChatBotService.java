package com.moa.service.chat;

import com.moa.dto.chat.request.ChatRequest;
import com.moa.dto.chat.response.ChatResponse;

public interface ChatBotService {
	ChatResponse chat(ChatRequest request);
}
