package com.moa.dto.push.request;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPushRequest {

	private String sendType;

	private List<String> receiverIds;

	private String pushCode;

	private Map<String, String> params;

	private String title;

	private String content;

	private String moduleId;

	private String moduleType;
}