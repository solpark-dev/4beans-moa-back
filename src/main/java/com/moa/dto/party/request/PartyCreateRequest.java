package com.moa.dto.party.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 파티 생성 요청 DTO
 *
 * v1.0 파티 생성 프로세스:
 * 1. 사용자가 이 DTO의 정보를 입력하여 파티 생성
 * 2. 시스템이 PARTY 테이블에 INSERT (STATUS: PENDING_PAYMENT)
 * 3. 방장이 즉시 보증금 결제 (월구독료 전액)
 * 4. 결제 완료 시 STATUS → RECRUITING으로 변경
 *
 * 필수 입력:
 * - productId: 상품 ID
 * - maxMembers: 최대 인원 (2~10명)
 * - startDate: 파티 시작일 (결제 기준일)
 * - endDate: 파티 종료일
 *
 * 선택 입력 (생성 시점):
 * - ottId: OTT 계정 ID (결제 후 입력)
 * - ottPassword: OTT 계정 비밀번호 (결제 후 입력)
 * - accountId: 정산 계좌 ID (나중에 등록 가능)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyCreateRequest {

    /**
     * 상품 ID (필수)
     */
    @NotNull(message = "상품 ID는 필수입니다.")
    private Integer productId;

    /**
     * 최대 인원 (필수, 2~10명)
     */
    @NotNull(message = "최대 인원은 필수입니다.")
    @Min(value = 2, message = "최대 인원은 최소 2명입니다.")
    @Max(value = 10, message = "최대 인원은 최대 10명입니다.")
    private Integer maxMembers;

    /**
     * 파티 시작일 (필수)
     * 모든 파티원의 월별 결제 기준일이 됨
     * 예: 2025-01-15 → 매월 15일에 자동 결제
     */
    @NotNull(message = "파티 시작일은 필수입니다.")
    private LocalDate startDate;

    /**
     * 파티 종료일 (선택)
     */
    private LocalDate endDate;

    /**
     * OTT 서비스 계정 ID (선택 - 생성 시점에는 없을 수 있음)
     * 예: Netflix 계정 이메일
     */
    @Size(max = 100, message = "OTT 계정 ID는 최대 100자까지 입력 가능합니다.")
    private String ottId;

    /**
     * OTT 서비스 계정 비밀번호 (선택 - 생성 시점에는 없을 수 있음)
     */
    @Size(max = 255, message = "OTT 계정 비밀번호는 최대 255자까지 입력 가능합니다.")
    private String ottPassword;

    /**
     * 정산 계좌 ID (선택)
     * 파티 생성 시 등록하거나, 나중에 추가 가능
     */
    private Integer accountId;
}