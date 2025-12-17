package com.moa.domain;

import java.sql.Date;

import lombok.Data;

@Data
public class Subscription {
    private int subscriptionId;
    private String userId;
    private int productId;
    private String subscriptionStatus;
    private Date startDate;
    private Date endDate;
    private String cancelReason;
    private Date cancelDate;
}
