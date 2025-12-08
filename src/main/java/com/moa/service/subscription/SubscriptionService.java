package com.moa.service.subscription;

import com.moa.dto.subscription.SubscriptionDTO;
import java.util.List;

public interface SubscriptionService {

    public void addSubscription(SubscriptionDTO subscriptionDTO) throws Exception;

    public SubscriptionDTO getSubscription(int subscriptionId) throws Exception;

    public List<SubscriptionDTO> getSubscriptionList() throws Exception;

    public void updateSubscription(SubscriptionDTO subscriptionDTO) throws Exception;

    public void cancelSubscription(int subscriptionId) throws Exception;
}
