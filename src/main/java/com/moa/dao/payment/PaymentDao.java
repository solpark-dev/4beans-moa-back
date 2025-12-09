package com.moa.dao.payment;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.Payment;
import com.moa.dto.payment.response.PaymentDetailResponse;
import com.moa.dto.payment.response.PaymentResponse;

@Mapper
public interface PaymentDao {

        int insertPayment(Payment payment);

        Optional<Payment> findById(@Param("paymentId") Integer paymentId);

        Optional<PaymentDetailResponse> findDetailById(@Param("paymentId") Integer paymentId);

        List<PaymentResponse> findByUserId(@Param("userId") String userId);

        List<PaymentResponse> findByPartyId(@Param("partyId") Integer partyId);

        Optional<Payment> findByPartyMemberIdAndTargetMonth(
                        @Param("partyMemberId") Integer partyMemberId,
                        @Param("targetMonth") String targetMonth);

        Optional<Payment> findByOrderId(@Param("orderId") String orderId);

        int updatePaymentStatus(
                        @Param("paymentId") Integer paymentId,
                        @Param("status") String status);

        Optional<Payment> findByPartyMemberIdAndType(
                        @Param("partyMemberId") Integer partyMemberId,
                        @Param("paymentType") String paymentType);
}