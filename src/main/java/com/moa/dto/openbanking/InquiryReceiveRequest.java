package com.moa.dto.openbanking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryReceiveRequest {
    
    @NotBlank(message = "은행코드는 필수입니다")
    @Size(min = 3, max = 3, message = "은행코드는 3자리입니다")
    private String bankCodeStd;
    
    @NotBlank(message = "계좌번호는 필수입니다")
    @Size(max = 20, message = "계좌번호는 20자 이내입니다")
    private String accountNum;
    
    @NotBlank(message = "예금주명은 필수입니다")
    @Size(max = 50, message = "예금주명은 50자 이내입니다")
    private String accountHolderInfo;
    
    @Builder.Default
    private String tranAmt = "1";
    private String printContent;
}
