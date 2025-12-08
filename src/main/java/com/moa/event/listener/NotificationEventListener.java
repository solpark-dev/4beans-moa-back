//package com.moa.event.listener;
//
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//
//import com.moa.common.event.RefundCompletedEvent;
//import com.moa.common.event.SettlementCompletedEvent;
//import com.moa.domain.Push;
//import com.moa.service.push.PushService;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationEventListener {
//
//    private final PushService pushService;
//
//    @EventListener
//    public void handleSettlementCompleted(SettlementCompletedEvent event) {
//        log.info("Handling SettlementCompletedEvent for partyId: {}", event.getPartyId());
//
//        String title = "정산 완료 알림";
//        String content = String.format("파티(ID:%d)의 정산금 %d원이 입금되었습니다.", event.getPartyId(), event.getAmount());
//
//        Push push = Push.builder()
//                .receiverId(event.getLeaderId())
//                .title(title)
//                .content(content)
//                .moduleType("SETTLEMENT")
//                .moduleId(String.valueOf(event.getPartyId()))
//                .build();
//
//        pushService.addPush(push); 
//    }
//
//    @EventListener
//    public void handleRefundCompleted(RefundCompletedEvent event) {
//        log.info("Handling RefundCompletedEvent for depositId: {}", event.getDepositId());
//
//        String title = "환불 완료 알림";
//        String content = String.format("보증금 %d원이 환불되었습니다.", event.getAmount());
//
//        Push push = Push.builder()
//                .receiverId(event.getUserId())
//                .title(title)
//                .content(content)
//                .moduleType("DEPOSIT")
//                .moduleId(String.valueOf(event.getDepositId()))
//                .build();
//
//        pushService.addPush(push);
//    }
//}
