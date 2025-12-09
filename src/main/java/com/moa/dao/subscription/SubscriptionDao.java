package com.moa.dao.subscription;

import com.moa.domain.Subscription;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.moa.dto.subscription.SubscriptionDTO; // Added this import for SubscriptionDTO

@Mapper
public interface SubscriptionDao {

    public void addSubscription(Subscription subscription) throws Exception;

    public SubscriptionDTO getSubscription(int subscriptionId) throws Exception;

    public List<SubscriptionDTO> getSubscriptionList(String userId) throws Exception;

    public void updateSubscription(SubscriptionDTO subscription) throws Exception; // Corrected parameter name for
                                                                                   // syntactical correctness
}
