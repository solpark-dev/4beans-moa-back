package com.moa.web.push;

import com.moa.dto.community.response.PageResponse;
import com.moa.dto.push.request.MultiPushRequest;
import com.moa.dto.push.request.PushRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.dto.push.response.PushResponse;
import com.moa.service.push.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushRestController {
    
    private final PushService pushService;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> addPush(@RequestBody PushRequest request) {
        pushService.addPush(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "푸시 알림이 발송되었습니다."));
    }
    
    @PostMapping("/multi")
    public ResponseEntity<Map<String, Object>> addPushMulti(@RequestBody MultiPushRequest request) {
        pushService.addPushMulti(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "푸시 알림이 일괄 발송되었습니다."));
    }
    
    @PostMapping("/template")
    public ResponseEntity<Map<String, Object>> addTemplatePush(@RequestBody TemplatePushRequest request) {
        pushService.addTemplatePush(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "템플릿 푸시 알림이 발송되었습니다."));
    }
    
    @GetMapping("/{pushId}")
    public ResponseEntity<PushResponse> getPush(@PathVariable Integer pushId) {
        PushResponse response = pushService.getPush(pushId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/list")
    public ResponseEntity<PageResponse<PushResponse>> getPushList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<PushResponse> response = pushService.getPushList(page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/my")
    public ResponseEntity<PageResponse<PushResponse>> getMyPushList(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<PushResponse> response = pushService.getMyPushList(userDetails.getUsername(), page, size);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        int count = pushService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("success", true, "count", count));
    }
    
    @PatchMapping("/{pushId}/read")
    public ResponseEntity<Map<String, Object>> updateRead(@PathVariable Integer pushId) {
        pushService.updateRead(pushId);
        return ResponseEntity.ok(Map.of("success", true, "message", "읽음 처리되었습니다."));
    }
    
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> updateAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        pushService.updateAllRead(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("success", true, "message", "전체 읽음 처리되었습니다."));
    }
    
    @DeleteMapping("/{pushId}")
    public ResponseEntity<Map<String, Object>> deletePush(@PathVariable Integer pushId) {
        pushService.deletePush(pushId);
        return ResponseEntity.ok(Map.of("success", true, "message", "삭제되었습니다."));
    }
    
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAllPushs(@AuthenticationPrincipal UserDetails userDetails) {
        pushService.deleteAllPushs(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("success", true, "message", "전체 삭제되었습니다."));
    }
}