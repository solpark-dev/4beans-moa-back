package com.moa.web.subscription;

import com.moa.dto.subscription.SubscriptionDTO;
import com.moa.service.subscription.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionRestController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping
    public void addSubscription(@RequestBody SubscriptionDTO subscriptionDTO) throws Exception {
        logger.debug("Request [addSubscription] Time: {}, Content: {}", java.time.LocalDateTime.now(), subscriptionDTO);
        subscriptionService.addSubscription(subscriptionDTO);
    }

    @GetMapping("/{subscriptionId}")
    public SubscriptionDTO getSubscription(@PathVariable int subscriptionId) throws Exception {
        logger.debug("Request [getSubscription] Time: {}, subscriptionId: {}", java.time.LocalDateTime.now(),
                subscriptionId);
        return subscriptionService.getSubscription(subscriptionId);
    }

    @GetMapping
    public List<SubscriptionDTO> getSubscriptionList() throws Exception {
        logger.debug("Request [getSubscriptionList] Time: {}", java.time.LocalDateTime.now());
        return subscriptionService.getSubscriptionList();
    }

    @PutMapping
    public void updateSubscription(@RequestBody SubscriptionDTO subscriptionDTO) throws Exception {
        logger.debug("Request [updateSubscription] Time: {}, Content: {}", java.time.LocalDateTime.now(),
                subscriptionDTO);
        subscriptionService.updateSubscription(subscriptionDTO);
    }

    @PostMapping("/{subscriptionId}/cancel")
    public void cancelSubscription(@PathVariable int subscriptionId) throws Exception {
        logger.debug("Request [cancelSubscription] Time: {}, subscriptionId: {}", java.time.LocalDateTime.now(),
                subscriptionId);
        subscriptionService.cancelSubscription(subscriptionId);
    }
}
