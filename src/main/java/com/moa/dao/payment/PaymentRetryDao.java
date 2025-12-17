package com.moa.dao.payment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.moa.domain.PaymentRetryHistory;

@Mapper
public interface PaymentRetryDao {

	int insert(PaymentRetryHistory history);

	Optional<PaymentRetryHistory> findById(@Param("retryId") Integer retryId);

	List<PaymentRetryHistory> findByPaymentId(@Param("paymentId") Integer paymentId);

	List<PaymentRetryHistory> findByNextRetryDate(@Param("today") LocalDate today);

	Optional<PaymentRetryHistory> findLatestByPaymentId(@Param("paymentId") Integer paymentId);
}
