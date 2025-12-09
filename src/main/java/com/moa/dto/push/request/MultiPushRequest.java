package com.moa.dto.push.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiPushRequest {
    
    private List<String> receiverIds;
    private String pushCode;
    private String title;
    private String content;
    private String moduleId;
    private String moduleType;
}