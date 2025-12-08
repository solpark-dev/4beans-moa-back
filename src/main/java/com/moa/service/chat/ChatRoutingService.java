package com.moa.service.chat;

import com.moa.domain.ChatRoute;

public interface ChatRoutingService {

	ChatRoute route(String text);
}
