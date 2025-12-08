package com.moa.dto.push.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplatePushRequest {
    
    private String receiverId;
    private String pushCode;
    private Map<String, String> params;
    private String moduleId;
    private String moduleType;
}