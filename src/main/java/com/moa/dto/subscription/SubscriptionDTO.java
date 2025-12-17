package com.moa.dto.subscription;

import com.moa.domain.Subscription;
import lombok.Data;
import java.sql.Date;

@Data
public class SubscriptionDTO {
    private int subscriptionId;
    private String userId;
    private int productId;
    private String subscriptionStatus;
    private Date startDate;
    private Date endDate;
    private String cancelReason;
    private Date cancelDate;

    private String productName;
    private String productImage;
    private int price;
    private String categoryName;

    public Subscription toEntity() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(this.subscriptionId);
        subscription.setUserId(this.userId);
        subscription.setProductId(this.productId);
        subscription.setSubscriptionStatus(this.subscriptionStatus);
        subscription.setStartDate(this.startDate);
        subscription.setEndDate(this.endDate);
        subscription.setCancelReason(this.cancelReason);
        subscription.setCancelDate(this.cancelDate);
        return subscription;
    }

    public static SubscriptionDTO fromEntity(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setSubscriptionId(subscription.getSubscriptionId());
        dto.setUserId(subscription.getUserId());
        dto.setProductId(subscription.getProductId());
        dto.setSubscriptionStatus(subscription.getSubscriptionStatus());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        dto.setCancelReason(subscription.getCancelReason());
        dto.setCancelDate(subscription.getCancelDate());
        return dto;
    }
}
