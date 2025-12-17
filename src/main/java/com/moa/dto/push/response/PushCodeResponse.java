package com.moa.dto.push.response;

import com.moa.domain.PushCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushCodeResponse {

    private Integer pushCodeId;
    private String codeName;
    private String titleTemplate;
    private String contentTemplate;
    private LocalDateTime createdAt;

    public static PushCodeResponse fromEntity(PushCode pushCode) {
        return PushCodeResponse.builder()
                .pushCodeId(pushCode.getPushCodeId())
                .codeName(pushCode.getCodeName())
                .titleTemplate(pushCode.getTitleTemplate())
                .contentTemplate(pushCode.getContentTemplate())
                .createdAt(pushCode.getCreatedAt())
                .build();
    }
}