package com.moa.dao.subscription;

import com.moa.domain.Subscription;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SubscriptionDao {

    public void addSubscription(Subscription subscription) throws Exception;

    public Subscription getSubscription(int subscriptionId) throws Exception;

    public List<Subscription> getSubscriptionList() throws Exception;

    public void updateSubscription(Subscription subscription) throws Exception;
}
