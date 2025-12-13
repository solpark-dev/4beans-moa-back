package com.moa.dto.push.request;

import com.moa.domain.PushCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 푸시 코드(템플릿) 생성/수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushCodeRequest {

    private String codeName;        // 푸시 코드명 (예: PAY_SUCCESS)
    private String titleTemplate;   // 제목 템플릿 (예: 결제 완료)
    private String contentTemplate; // 내용 템플릿 (예: {productName} 결제가 완료되었습니다.)

    public PushCode toEntity() {
        return PushCode.builder()
                .codeName(this.codeName)
                .titleTemplate(this.titleTemplate)
                .contentTemplate(this.contentTemplate)
                .build();
    }
}