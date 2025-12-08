package com.moa.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushCode {
    private Integer pushCodeId;
    private String codeName;
    private String titleTemplate;
    private String contentTemplate;
    private LocalDateTime createdAt;
}