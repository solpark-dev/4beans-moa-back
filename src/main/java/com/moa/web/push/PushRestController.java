package com.moa.web.push;

import com.moa.dto.community.response.PageResponse;
import com.moa.dto.push.request.AdminPushRequest;
import com.moa.dto.push.request.MultiPushRequest;
import com.moa.dto.push.request.PushCodeRequest;
import com.moa.dto.push.request.PushRequest;
import com.moa.dto.push.request.TemplatePushRequest;
import com.moa.dto.push.response.PushCodeResponse;
import com.moa.dto.push.response.PushResponse;
import com.moa.service.push.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushRestController {

    private final PushService pushService;

    // ===== 일반 푸시 API =====

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

    // ===== 관리자용 API =====

    /**
     * 푸시코드 템플릿 전체 목록 조회
     */
    @GetMapping("/admin/codes")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<PushCodeResponse>> getPushCodeList() {
        List<PushCodeResponse> list = pushService.getPushCodeList();
        return ResponseEntity.ok(list);
    }

    /**
     * 푸시코드 템플릿 상세 조회
     */
    @GetMapping("/admin/codes/{pushCodeId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PushCodeResponse> getPushCode(@PathVariable Integer pushCodeId) {
        PushCodeResponse response = pushService.getPushCode(pushCodeId);
        return ResponseEntity.ok(response);
    }

    /**
     * 푸시코드 템플릿 추가
     */
    @PostMapping("/admin/codes")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> addPushCode(@RequestBody PushCodeRequest request) {
        pushService.addPushCode(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "푸시 템플릿이 추가되었습니다."));
    }

    /**
     * 푸시코드 템플릿 수정
     */
    @PutMapping("/admin/codes/{pushCodeId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePushCode(
            @PathVariable Integer pushCodeId,
            @RequestBody PushCodeRequest request) {
        pushService.updatePushCode(pushCodeId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "푸시 템플릿이 수정되었습니다."));
    }

    /**
     * 푸시코드 템플릿 삭제
     */
    @DeleteMapping("/admin/codes/{pushCodeId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePushCode(@PathVariable Integer pushCodeId) {
        pushService.deletePushCode(pushCodeId);
        return ResponseEntity.ok(Map.of("success", true, "message", "푸시 템플릿이 삭제되었습니다."));
    }

    /**
     * 전체 푸시 발송 내역 조회 (필터 가능)
     */
    @GetMapping("/admin/history")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageResponse<PushResponse>> getPushHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String pushCode,
            @RequestParam(required = false) String receiverId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PageResponse<PushResponse> response = pushService.getPushHistory(page, size, pushCode, receiverId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자 수동 푸시 발송
     */
    @PostMapping("/admin/send")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendAdminPush(@RequestBody AdminPushRequest request) {
        int count = pushService.sendAdminPush(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", count + "명에게 푸시 알림이 발송되었습니다.",
                "count", count
        ));
    }

    /**
     * 수신자 검색 (사용자 목록)
     */
    @GetMapping("/admin/search")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Map<String, String>>> searchUsers(
            @RequestParam(required = false) String keyword) {
        List<Map<String, String>> users = pushService.searchUsersForPush(keyword);
        return ResponseEntity.ok(users);
    }
}