package com.moa.dao.subscription;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.moa.domain.Subscription;
import com.moa.dto.subscription.SubscriptionDTO;

@Mapper
public interface SubscriptionDao {

	public void addSubscription(Subscription subscription) throws Exception;

	public SubscriptionDTO getSubscription(int subscriptionId) throws Exception;

	public List<SubscriptionDTO> getSubscriptionList(String userId) throws Exception;

	public void updateSubscription(SubscriptionDTO subscription) throws Exception;
}
