package com.moa.dto.community.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class InquiryRequest {

    @NotBlank
    private String userId;

    @NotNull
    private Integer communityCodeId;

    @NotBlank
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;

    @NotBlank
    @Size(max = 500, message = "내용은 500자를 초과할 수 없습니다.")
    private String content;

    private MultipartFile file;
}
