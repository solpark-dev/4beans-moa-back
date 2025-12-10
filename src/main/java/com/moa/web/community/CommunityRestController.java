package com.moa.web.community;

import com.moa.dto.community.request.*;
import com.moa.dto.community.response.*;
import com.moa.service.community.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityRestController {
    
    private final CommunityService communityService;
    
    @Value("${app.upload.community.inquiry-dir}")
    private String inquiryUploadDir;
    
    @GetMapping("/notice")
    public ResponseEntity<PageResponse<NoticeResponse>> getNoticeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(communityService.getNoticeList(page, size));
    }
    
    @GetMapping("/notice/{communityId}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable Integer communityId) {
        return ResponseEntity.ok(communityService.getNotice(communityId));
    }
    
    @PostMapping("/notice")
    public ResponseEntity<Void> addNotice(@RequestBody NoticeRequest request) {
        communityService.addNotice(request);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/notice/{communityId}")
    public ResponseEntity<Void> updateNotice(
            @PathVariable Integer communityId,
            @RequestBody NoticeRequest request) {
        communityService.updateNotice(communityId, request);
        return ResponseEntity.ok().build();
    }
    
//    @DeleteMapping("/notice/{communityId}")
//    public ResponseEntity<Void> deleteNotice(@PathVariable Integer communityId) {
//        communityService.deleteNotice(communityId);
//        return ResponseEntity.ok().build();
//    }
    
    @GetMapping("/notice/search")
    public ResponseEntity<PageResponse<NoticeResponse>> searchNotice(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(communityService.searchNotice(keyword, page, size));
    }
    
    @GetMapping("/faq")
    public ResponseEntity<PageResponse<FaqResponse>> getFaqList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(communityService.getFaqList(page, size));
    }
    
    @GetMapping("/faq/{communityId}")
    public ResponseEntity<FaqResponse> getFaq(@PathVariable Integer communityId) {
        return ResponseEntity.ok(communityService.getFaq(communityId));
    }
    
    @PostMapping("/faq")
    public ResponseEntity<Void> addFaq(@RequestBody FaqRequest request) {
        communityService.addFaq(request);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/faq/{communityId}")
    public ResponseEntity<Void> updateFaq(
            @PathVariable Integer communityId,
            @RequestBody FaqRequest request) {
        communityService.updateFaq(communityId, request);
        return ResponseEntity.ok().build();
    }
    
//    @DeleteMapping("/faq/{communityId}")
//    public ResponseEntity<Void> deleteFaq(@PathVariable Integer communityId) {
//        communityService.deleteFaq(communityId);
//        return ResponseEntity.ok().build();
//    }
    
    @GetMapping("/faq/search")
    public ResponseEntity<PageResponse<FaqResponse>> searchFaq(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(communityService.searchFaq(keyword, page, size));
    }
    
    @GetMapping("/inquiry/my")
    public ResponseEntity<PageResponse<InquiryResponse>> getMyInquiryList(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(communityService.getMyInquiryList(userId, page, size));
    }
    
    @GetMapping("/inquiry")
    public ResponseEntity<PageResponse<InquiryResponse>> getInquiryList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(communityService.getInquiryList(page, size));
    }
    
    @GetMapping("/inquiry/{communityId}")
    public ResponseEntity<InquiryResponse> getInquiry(@PathVariable Integer communityId) {
        return ResponseEntity.ok(communityService.getInquiry(communityId));
    }
    
    @GetMapping("/inquiry/image/{fileUuid}")
    public ResponseEntity<Resource> getInquiryImage(@PathVariable String fileUuid) {
        try {
            Path filePath = Paths.get(inquiryUploadDir).resolve(fileUuid);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = "image/png";
                if (fileUuid.toLowerCase().endsWith(".jpg") || fileUuid.toLowerCase().endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                }
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/inquiry")
    public ResponseEntity<Void> addInquiry(
            @RequestParam String userId,
            @RequestParam Integer communityCodeId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile file) {
        communityService.addInquiry(userId, communityCodeId, title, content, file);
        return ResponseEntity.ok().build();
    }
    
//    @DeleteMapping("/inquiry/{communityId}")
//    public ResponseEntity<Void> deleteInquiry(@PathVariable Integer communityId) {
//        communityService.deleteInquiry(communityId);
//        return ResponseEntity.ok().build();
//    }
    
    @PostMapping("/inquiry/answer")
    public ResponseEntity<Void> addAnswer(@RequestBody AnswerRequest request) {
        communityService.addAnswer(request);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/inquiry/answer")
    public ResponseEntity<Void> updateAnswer(@RequestBody AnswerRequest request) {
        communityService.updateAnswer(request);
        return ResponseEntity.ok().build();
    }
}