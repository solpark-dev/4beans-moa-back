package com.moa.service.subscription.impl;

import com.moa.dao.subscription.SubscriptionDao;
import com.moa.domain.Subscription;
import com.moa.dto.subscription.SubscriptionDTO;
import com.moa.service.subscription.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Override
    public void addSubscription(SubscriptionDTO subscriptionDTO) throws Exception {
        subscriptionDao.addSubscription(subscriptionDTO.toEntity());
    }

    @Override
    public SubscriptionDTO getSubscription(int subscriptionId) throws Exception {
        Subscription subscription = subscriptionDao.getSubscription(subscriptionId);
        return SubscriptionDTO.fromEntity(subscription);
    }

    @Override
    public List<SubscriptionDTO> getSubscriptionList() throws Exception {
        List<Subscription> subscriptionList = subscriptionDao.getSubscriptionList();
        return subscriptionList.stream()
                .map(SubscriptionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void updateSubscription(SubscriptionDTO subscriptionDTO) throws Exception {
        subscriptionDao.updateSubscription(subscriptionDTO.toEntity());
    }

    @Override
    public void cancelSubscription(int subscriptionId) throws Exception {
        Subscription subscription = subscriptionDao.getSubscription(subscriptionId);
        if (subscription != null) {
            subscription.setSubscriptionStatus("CANCELLED");
            subscriptionDao.updateSubscription(subscription);
        }
    }
}
