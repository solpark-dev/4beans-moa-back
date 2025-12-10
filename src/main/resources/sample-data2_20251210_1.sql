-- ============================================
-- OTT 구독 공유 서비스 MOA 샘플 데이터
-- Version: 5.1 (데이터 무결성 개선 + 최적화)
-- 작성일: 2025.12.10
-- 변경사항:
--   - PARTY 테이블에서 LEADER_DEPOSIT_ID 제거
--   - PARTY_MEMBER 테이블에서 DEPOSIT_ID, FIRST_PAYMENT_ID 제거
--   - SETTLEMENT_RETRY_HISTORY에서 PARTY_ID, PARTY_LEADER_ID, ACCOUNT_ID 제거
--   - SETTLEMENT_DETAIL 테이블 완전 제거
--   - 전체 26명 OTP 비활성화 (로그인 편의성 확보)
--   - 양방향 참조 해소 및 정규화
-- ============================================

USE moa;

-- ============================================
-- 1. 초기화 (기존 데이터 삭제)
-- ============================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE TRANSFER_TRANSACTION;
TRUNCATE TABLE ACCOUNT_VERIFICATION;
TRUNCATE TABLE SETTLEMENT_RETRY_HISTORY;
TRUNCATE TABLE REFUND_RETRY_HISTORY;
TRUNCATE TABLE PAYMENT_RETRY_HISTORY;
TRUNCATE TABLE SETTLEMENT;
TRUNCATE TABLE PAYMENT;
TRUNCATE TABLE DEPOSIT;
TRUNCATE TABLE PARTY_MEMBER;
TRUNCATE TABLE PARTY;
TRUNCATE TABLE SUBSCRIPTION;
TRUNCATE TABLE USER_CARD;
TRUNCATE TABLE ACCOUNT;
TRUNCATE TABLE PUSH;
TRUNCATE TABLE COMMUNITY;
TRUNCATE TABLE CHATBOT_KNOWLEDGE;
TRUNCATE TABLE LOGIN_HISTORY;
TRUNCATE TABLE USER_OTP_BACKUP_CODE;
TRUNCATE TABLE BLACKLIST;
TRUNCATE TABLE OAUTH_ACCOUNT;
TRUNCATE TABLE USERS;
TRUNCATE TABLE PRODUCT;
TRUNCATE TABLE CATEGORY;
TRUNCATE TABLE PUSH_CODE;
TRUNCATE TABLE COMMUNITY_CODE;
TRUNCATE TABLE BANK_CODE;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 2. 코드성 데이터
-- ============================================

-- BANK_CODE: 은행 코드 참조 데이터
INSERT INTO BANK_CODE (BANK_CODE, BANK_NAME, IS_ACTIVE) VALUES
('004', 'KB국민은행', 'Y'),
('011', 'NH농협은행', 'Y'),
('020', '우리은행', 'Y'),
('023', 'SC제일은행', 'Y'),
('027', '한국씨티은행', 'Y'),
('032', '대구은행', 'Y'),
('034', '광주은행', 'Y'),
('035', '제주은행', 'Y'),
('037', '전북은행', 'Y'),
('039', '경남은행', 'Y'),
('045', '새마을금고', 'Y'),
('048', '신협', 'Y'),
('071', '우체국', 'Y'),
('081', '하나은행', 'Y'),
('088', '신한은행', 'Y'),
('089', '케이뱅크', 'Y'),
('090', '카카오뱅크', 'Y'),
('092', '토스뱅크', 'Y'),
('003', 'IBK기업은행', 'Y')
ON DUPLICATE KEY UPDATE BANK_NAME = VALUES(BANK_NAME);

-- COMMUNITY_CODE: 커뮤니티 카테고리
INSERT INTO COMMUNITY_CODE (COMMUNITY_CODE_ID, CATEGORY, CODE_NAME) VALUES
(1, 'INQUIRY', '회원'),
(2, 'INQUIRY', '결제'),
(3, 'INQUIRY', '기타'),
(4, 'POST', 'FAQ'),
(5, 'POST', '회원'),
(6, 'POST', '결제'),
(7, 'POST', '구독'),
(8, 'POST', '파티'),
(9, 'POST', '정산'),
(10, 'POST', '시스템');

-- PUSH_CODE: 푸시 알림 템플릿
INSERT INTO PUSH_CODE (CODE_NAME, TITLE_TEMPLATE, CONTENT_TEMPLATE) VALUES
('INQUIRY_ANSWER', '문의 답변 완료', '{nickname}님이 남기신 문의에 답변이 등록되었습니다.'),
('PAYMENT_SUCCESS', '결제 완료', '{nickname}님의 {product_name} 파티 월회비 {amount}원 결제가 완료되었습니다.'),
('PAYMENT_FAIL', '결제 실패', '{nickname}님의 {product_name} 파티 결제가 실패했습니다. 결제 수단을 확인해주세요.'),
('PARTY_JOIN', '파티 가입 완료', '{nickname}님, {product_name} 파티에 성공적으로 참여하셨습니다.'),
('PARTY_WITHDRAW', '파티 탈퇴 완료', '{nickname}님, {product_name} 파티에서 탈퇴 처리되었습니다.'),
('PARTY_START', '파티 시작', '{product_name} 파티가 시작되었습니다. OTT 계정 정보를 확인하세요.'),
('PARTY_END', '파티 종료', '{product_name} 파티가 종료되었습니다.'),
('SETTLEMENT_MONTHLY', '월간 정산 완료', '{month}월 정산 금액 {amount}원이 입금될 예정입니다.'),
('DEPOSIT_PAID', '보증금 납부 완료', '{product_name} 파티 보증금 {amount}원 납부가 완료되었습니다.'),
('DEPOSIT_REFUND', '보증금 환불 완료', '{product_name} 파티 보증금 {amount}원이 환불되었습니다.'),
('PAYMENT_RETRY', '결제 재시도 중', '{nickname}님의 {product_name} 파티 결제가 실패하여 재시도 중입니다. (시도: {attempt_number}/4)'),
('PAYMENT_RETRY_SUCCESS', '결제 재시도 성공', '{nickname}님의 {product_name} 파티 결제가 {attempt_number}회 시도 만에 성공했습니다.'),
('PAYMENT_RETRY_FINAL_FAIL', '결제 최종 실패', '{nickname}님의 {product_name} 파티 결제가 4회 시도 후 최종 실패했습니다. 결제 수단을 확인해주세요.');

-- CATEGORY: 상품 카테고리
INSERT INTO CATEGORY (CATEGORY_ID, CATEGORY_NAME) VALUES
(1, 'AI'),
(2, 'MEDIA'),
(3, 'EDU'),
(4, 'MEMBER');

-- PRODUCT: 상품 정보
INSERT INTO `product` (`PRODUCT_ID`, `CATEGORY_ID`, `PRODUCT_NAME`, `PRODUCT_STATUS`, `PRICE`, `IMAGE`, `MAX_SHARE`) VALUES
   (1, 1, 'Google AI Pro', 'ACTIVE', 17000, '/uploads/product-image/googleaipro_icon.png', NULL),
   (2, 2, 'Disney+ Standard', 'ACTIVE', 9900, '/uploads/product-image/disney_plus_icon.png', NULL),
   (3, 2, '왓챠 베이직', 'ACTIVE', 7900, '/uploads/product-image/watcha_icon.png', NULL),
   (4, 2, '유튜브 프리미엄', 'ACTIVE', 13900, '/uploads/product-image/YouTube_icon.png', NULL),
   (5, 1, 'Chat GPT Plus', 'ACTIVE', 29000, '/uploads/product-image/chatgpt_icon.png', NULL),
   (6, 2, '쿠팡플레이WOW', 'ACTIVE', 7890, '/uploads/product-image/coupangplay_icon.png', NULL),
   (7, 2, '티빙 스탠다드', 'ACTIVE', 10900, '/uploads/product-image/tving_icon.png', NULL),
   (8, 2, '웨이브 프리미엄', 'ACTIVE', 13900, '/uploads/product-image/wavve_icon.png', NULL),
   (9, 4, 'Naver 멤버십 1개월권', 'ACTIVE', 3000, '/uploads/product-image/naver_member_icon.png', NULL),
   (10, 4, 'Naver 멤버십 12개월권', 'ACTIVE', 30000, '/uploads/product-image/naver_member_icon.png', NULL),
   (11, 1, 'Chat GPT Pro', 'ACTIVE', 50000, '/uploads/product-image/chatgpt_icon.png', NULL),
   (12, 1, 'Google AI Ultra', 'ACTIVE', 330000, '/uploads/product-image/googleaipro_icon.png', NULL),
   (13, 2, 'Disney+ Premium', 'ACTIVE', 13900, '/uploads/product-image/disney_plus_icon.png', NULL),
   (14, 2, '쿠팡플레이 Sports Pass', 'ACTIVE', 16600, '/uploads/product-image/coupangplay_icon.png', NULL),
   (15, 3, 'Skillshare Monthly', 'ACTIVE', 20600, '/uploads/product-image/skillshare_icon.png', NULL),
   (16, 3, 'LinkedIn Learning Monthly', 'ACTIVE', 58900, '/uploads/product-image/LinkedInlearning_icon.png', NULL),
   (17, 2, 'Disney+ + TVING Bundle', 'ACTIVE', 18000, '/uploads/product-image/disney_plus_tving_icon.png', NULL),
   (18, 2, 'Netflix Basic', 'ACTIVE', 9500, '/uploads/product-image/netflix_icon.png', NULL),
   (19, 2, 'Netflix Standard', 'ACTIVE', 14500, '/uploads/product-image/netflix_icon.png', NULL),
   (20, 2, 'Netflix Premium', 'ACTIVE', 19000, '/uploads/product-image/netflix_icon.png', NULL),
   (21, 2, 'Netflix ', 'ACTIVE', 19000, '/uploads/product-image/netflix_icon.png', NULL);

-- CHATBOT_KNOWLEDGE: 챗봇 지식 베이스 데이터
INSERT INTO CHATBOT_KNOWLEDGE (CATEGORY, TITLE, QUESTION, ANSWER, KEYWORDS) VALUES 
('구독', '구독상품 안내', '구독상품이 뭐가 있나요?', 'MoA에서는 OTT, 음악, 게임 등 다양한 구독상품을 제공해요.', '구독,상품,OTT,음악,게임'),
('결제', '결제 수단 변경', '결제 카드를 바꾸고 싶어요', '마이페이지 > 결제 관리에서 카드 추가/변경이 가능해요.', '결제,카드,변경,마이페이지'),
('파티', '파티 가입 방법', '파티는 어떻게 가입하나요?', '원하는 OTT 서비스를 선택한 후 모집 중인 파티에 가입 신청을 하시면 됩니다.', '파티,가입,방법'),
('보증금', '보증금 환불 시기', '보증금은 언제 돌려받나요?', '파티 정상 종료 시 7일 이내에 보증금이 환불됩니다.', '보증금,환불,시기'),
('정산', '정산 일정', '정산은 언제 되나요?', '매월 5일에 전월 정산금이 등록하신 계좌로 입금됩니다.', '정산,일정,입금');

-- 1536차원 더미 임베딩 생성
SET SQL_SAFE_UPDATES = 0;

DROP PROCEDURE IF EXISTS make_dummy_embedding;
DELIMITER //
CREATE PROCEDURE make_dummy_embedding()
BEGIN
    DECLARE i INT DEFAULT 0;
    SET @vec = JSON_ARRAY();
    WHILE i < 1536 DO
        SET @vec = JSON_ARRAY_APPEND(@vec, '$', 0.0);
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL make_dummy_embedding();

UPDATE CHATBOT_KNOWLEDGE
SET EMBEDDING = @vec
WHERE EMBEDDING IS NULL AND ID > 0;

-- ============================================
-- 3. 회원 데이터 (다양한 가입 시나리오)
-- ============================================

-- USERS: 관리자 3명 + 일반회원 22명
-- ⭐ 전체 26명 OTP 비활성화 (OTP_SECRET = NULL, OTP_ENABLED = 0)
INSERT INTO USERS (
    USER_ID, PASSWORD, NICKNAME, PHONE,
    PROFILE_IMAGE, ROLE, USER_STATUS, REG_DATE,
    CI, PASS_CERTIFIED_AT, LAST_LOGIN_DATE,
    LOGIN_FAIL_COUNT, UNLOCK_SCHEDULED_AT,
    DELETE_DATE, DELETE_TYPE, DELETE_DETAIL, 
    AGREE_MARKETING, PROVIDER, OTP_SECRET, OTP_ENABLED
) VALUES
-- 관리자 계정 (모두 LOCAL 가입)
('admin@admin.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '슈퍼관리자', '01099999999', '/img/profile/super_admin.png', 'ADMIN', 'ACTIVE', '2024-01-01 00:00:00', 'CI_SUPER_ADMIN', '2024-01-01 00:00:00', '2024-12-03 09:00:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('admin@moa.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '관리자', '01000000000', '/img/profile/admin.png', 'ADMIN', 'ACTIVE', '2024-01-01 00:00:00', 'CI_ADMIN_001', '2024-01-01 00:00:00', '2024-12-03 08:00:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('admintest', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '테스트관리자1', '01000000000', NULL, 'ADMIN', 'ACTIVE', '2024-01-01 00:00:00', 'CI_ADMIN_011', '2024-01-01 00:00:00', '2024-12-03 08:00:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),

-- 소셜 전용 가입 (PASSWORD 있음)
('user001@gmail.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자001', '01010010001', NULL, 'USER', 'ACTIVE', '2024-03-01 10:30:00', 'CI_USER_001', '2024-03-01 10:30:00', '2024-11-28 14:20:00', 0, NULL, NULL, NULL, NULL, 1, 'KAKAO', NULL, 0),
('user002@naver.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자002', '01010010002', NULL, 'USER', 'ACTIVE', '2024-03-05 11:00:00', 'CI_USER_002', '2024-03-05 11:00:00', '2024-11-29 09:15:00', 0, NULL, NULL, NULL, NULL, 0, 'GOOGLE', NULL, 0),

-- 일반 가입 후 소셜 연동 (PASSWORD 있음)
('user003@daum.net', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자003', '01010010003', NULL, 'USER', 'ACTIVE', '2024-03-10 14:20:00', 'CI_USER_003', '2024-03-10 14:20:00', '2024-11-30 16:45:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user004@gmail.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자004', '01010010004', NULL, 'USER', 'ACTIVE', '2024-03-15 09:45:00', 'CI_USER_004', '2024-03-15 09:45:00', '2024-11-27 11:30:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user005@naver.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자005', '01010010005', NULL, 'USER', 'ACTIVE', '2024-03-20 16:10:00', 'CI_USER_005', '2024-03-20 16:10:00', '2024-11-28 18:20:00', 0, NULL, NULL, NULL, NULL, 0, 'LOCAL', NULL, 0),
('user006@daum.net', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자006', '01010010006', NULL, 'USER', 'ACTIVE', '2024-03-25 13:30:00', 'CI_USER_006', '2024-03-25 13:30:00', '2024-11-29 10:50:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user007@gmail.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자007', '01010010007', NULL, 'USER', 'ACTIVE', '2024-04-01 10:00:00', 'CI_USER_007', '2024-04-01 10:00:00', '2024-11-30 15:40:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user008@naver.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자008', '01010010008', NULL, 'USER', 'ACTIVE', '2024-04-05 11:20:00', 'CI_USER_008', '2024-04-05 11:20:00', '2024-11-28 13:10:00', 0, NULL, NULL, NULL, NULL, 0, 'LOCAL', NULL, 0),
('user009@daum.net', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자009', '01010010009', NULL, 'USER', 'ACTIVE', '2024-04-10 15:45:00', 'CI_USER_009', '2024-04-10 15:45:00', '2024-11-29 17:25:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user010@gmail.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자010', '01010010010', NULL, 'USER', 'ACTIVE', '2024-04-15 09:30:00', 'CI_USER_010', '2024-04-15 09:30:00', '2024-11-30 12:15:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),

-- 순수 일반 가입 (PASSWORD 있음, 소셜 연동 없음)
('user011@naver.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자011', '01010010011', NULL, 'USER', 'ACTIVE', '2024-04-20 14:15:00', 'CI_USER_011', '2024-04-20 14:15:00', '2024-11-28 16:35:00', 0, NULL, NULL, NULL, NULL, 0, 'LOCAL', NULL, 0),
('user012@daum.net', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자012', '01010010012', NULL, 'USER', 'ACTIVE', '2024-04-25 10:50:00', 'CI_USER_012', '2024-04-25 10:50:00', '2024-11-29 14:20:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user013@gmail.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자013', '01010010013', NULL, 'USER', 'ACTIVE', '2024-05-01 13:40:00', 'CI_USER_013', '2024-05-01 13:40:00', '2024-11-30 11:55:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user014@naver.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자014', '01010010014', NULL, 'USER', 'ACTIVE', '2024-05-05 16:25:00', 'CI_USER_014', '2024-05-05 16:25:00', '2024-11-28 09:40:00', 0, NULL, NULL, NULL, NULL, 0, 'LOCAL', NULL, 0),
('user015@daum.net', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자015', '01010010015', NULL, 'USER', 'ACTIVE', '2024-05-10 11:10:00', 'CI_USER_015', '2024-05-10 11:10:00', '2024-11-29 15:30:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user016@gmail.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자016', '01010010016', NULL, 'USER', 'ACTIVE', '2024-05-15 14:55:00', 'CI_USER_016', '2024-05-15 14:55:00', '2024-11-30 10:20:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user017@naver.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자017', '01010010017', NULL, 'USER', 'ACTIVE', '2024-05-20 09:20:00', 'CI_USER_017', '2024-05-20 09:20:00', '2024-11-28 12:45:00', 0, NULL, NULL, NULL, NULL, 0, 'LOCAL', NULL, 0),
('user018@daum.net', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자018', '01010010018', NULL, 'USER', 'ACTIVE', '2024-05-25 12:35:00', 'CI_USER_018', '2024-05-25 12:35:00', '2024-11-29 16:10:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user019@gmail.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자019', '01010010019', NULL, 'USER', 'ACTIVE', '2024-06-01 15:15:00', 'CI_USER_019', '2024-06-01 15:15:00', '2024-11-30 14:30:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('user020@naver.com', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자020', '01010010020', NULL, 'USER', 'ACTIVE', '2024-06-05 10:40:00', 'CI_USER_020', '2024-06-05 10:40:00', '2024-11-28 11:25:00', 0, NULL, NULL, NULL, NULL, 0, 'LOCAL', NULL, 0),

-- 테스트 계정
('usertest1', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '테스트사용자1', '01010010001', NULL, 'USER', 'ACTIVE', '2024-03-01 10:30:00', 'CI_USER_TEST1', '2024-03-01 10:30:00', '2024-11-28 14:20:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('usertest2', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '테스트사용자2', '01010010002', NULL, 'USER', 'ACTIVE', '2024-03-01 10:30:00', 'CI_USER_TEST2', '2024-03-01 10:30:00', '2024-11-28 14:20:00', 0, NULL, NULL, NULL, NULL, 1, 'LOCAL', NULL, 0),
('kjw', '$2a$10$r4WvD.fkTss4amaWwy7/dOV1SmwrMM.GocYPXsfgTL4td2mqrHZP6', '사용자aaa', '01010010099', NULL, 'USER', 'ACTIVE', '2024-06-05 10:40:00', 'CI_USER_kjw', '2024-06-05 10:40:00', '2025-12-05 14:45:59', 0, NULL, NULL, NULL, NULL, 0, 'LOCAL', NULL, 0);

-- OAUTH_ACCOUNT: 소셜 로그인 연동 (10명)
INSERT INTO OAUTH_ACCOUNT (
    OAUTH_ID, PROVIDER, PROVIDER_USER_ID, USER_ID, CONNECTED_DATE, RELEASE_DATE
) VALUES
('kakao_001_user001@gmail.com', 'KAKAO', 'kakao_uid_001', 'user001@gmail.com', '2024-03-01 10:30:00', NULL),
('google_002_user002@naver.com', 'GOOGLE', 'google_uid_002', 'user002@naver.com', '2024-03-05 11:00:00', NULL),
('kakao_003_user003@daum.net', 'KAKAO', 'kakao_uid_003', 'user003@daum.net', '2024-03-10 14:25:00', NULL),
('google_004_user004@gmail.com', 'GOOGLE', 'google_uid_004', 'user004@gmail.com', '2024-03-15 09:50:00', NULL),
('kakao_005_user005@naver.com', 'KAKAO', 'kakao_uid_005', 'user005@naver.com', '2024-03-20 16:15:00', NULL),
('google_006_user006@daum.net', 'GOOGLE', 'google_uid_006', 'user006@daum.net', '2024-03-25 13:35:00', NULL),
('kakao_007_user007@gmail.com', 'KAKAO', 'kakao_uid_007', 'user007@gmail.com', '2024-04-01 10:05:00', NULL),
('google_008_user008@naver.com', 'GOOGLE', 'google_uid_008', 'user008@naver.com', '2024-04-05 11:25:00', NULL),
('kakao_009_user009@daum.net', 'KAKAO', 'kakao_uid_009', 'user009@daum.net', '2024-04-10 15:50:00', NULL),
('google_010_user010@gmail.com', 'GOOGLE', 'google_uid_010', 'user010@gmail.com', '2024-04-15 09:35:00', NULL);

-- BLACKLIST: 블랙리스트 이력 (2명, 해제됨)
INSERT INTO BLACKLIST (
    USER_ID, REASON, STATUS, REG_DATE, RELEASE_DATE
) VALUES
('user001@gmail.com', '결제 실패 후 서비스 무단 이용 시도', 'RELEASE', '2024-04-01', '2024-04-15'),
('user005@naver.com', '파티 규칙 미준수 1회 경고', 'RELEASE', '2024-05-01', '2024-05-15');

-- ============================================
-- 4. 계좌/카드/구독 데이터
-- ============================================

-- ACCOUNT: 정산 계좌 정보 (20명)
INSERT INTO ACCOUNT (
    USER_ID, BANK_CODE, BANK_NAME, ACCOUNT_NUMBER,
    ACCOUNT_HOLDER, IS_VERIFIED, REG_DATE, VERIFY_DATE
) VALUES
('user001@gmail.com', '004', 'KB국민은행', 'ENC_100100010001', '사용자001', 'Y', '2024-03-02 10:00:00', '2024-03-02 10:30:00'),
('user002@naver.com', '088', '신한은행', 'ENC_100100010002', '사용자002', 'Y', '2024-03-06 11:00:00', '2024-03-06 11:30:00'),
('user003@daum.net', '020', '우리은행', 'ENC_100100010003', '사용자003', 'Y', '2024-03-11 14:00:00', '2024-03-11 14:30:00'),
('user004@gmail.com', '081', '하나은행', 'ENC_100100010004', '사용자004', 'Y', '2024-03-16 09:00:00', '2024-03-16 09:30:00'),
('user005@naver.com', '003', 'IBK기업은행', 'ENC_100100010005', '사용자005', 'Y', '2024-03-21 16:00:00', '2024-03-21 16:30:00'),
('user006@daum.net', '011', 'NH농협은행', 'ENC_100100010006', '사용자006', 'Y', '2024-03-26 13:00:00', '2024-03-26 13:30:00'),
('user007@gmail.com', '004', 'KB국민은행', 'ENC_100100010007', '사용자007', 'Y', '2024-04-02 10:00:00', '2024-04-02 10:30:00'),
('user008@naver.com', '088', '신한은행', 'ENC_100100010008', '사용자008', 'Y', '2024-04-06 11:00:00', '2024-04-06 11:30:00'),
('user009@daum.net', '020', '우리은행', 'ENC_100100010009', '사용자009', 'Y', '2024-04-11 15:00:00', '2024-04-11 15:30:00'),
('user010@gmail.com', '081', '하나은행', 'ENC_100100010010', '사용자010', 'Y', '2024-04-16 09:00:00', '2024-04-16 09:30:00'),
('user011@naver.com', '003', 'IBK기업은행', 'ENC_100100010011', '사용자011', 'Y', '2024-04-21 14:00:00', '2024-04-21 14:30:00'),
('user012@daum.net', '011', 'NH농협은행', 'ENC_100100010012', '사용자012', 'Y', '2024-04-26 10:00:00', '2024-04-26 10:30:00'),
('user013@gmail.com', '004', 'KB국민은행', 'ENC_100100010013', '사용자013', 'Y', '2024-05-02 13:00:00', '2024-05-02 13:30:00'),
('user014@naver.com', '088', '신한은행', 'ENC_100100010014', '사용자014', 'Y', '2024-05-06 16:00:00', '2024-05-06 16:30:00'),
('user015@daum.net', '020', '우리은행', 'ENC_100100010015', '사용자015', 'Y', '2024-05-11 11:00:00', '2024-05-11 11:30:00'),
('user016@gmail.com', '081', '하나은행', 'ENC_100100010016', '사용자016', 'Y', '2024-05-16 14:00:00', '2024-05-16 14:30:00'),
('user017@naver.com', '003', 'IBK기업은행', 'ENC_100100010017', '사용자017', 'Y', '2024-05-21 09:00:00', '2024-05-21 09:30:00'),
('user018@daum.net', '011', 'NH농협은행', 'ENC_100100010018', '사용자018', 'Y', '2024-05-26 12:00:00', '2024-05-26 12:30:00'),
('user019@gmail.com', '004', 'KB국민은행', 'ENC_100100010019', '사용자019', 'Y', '2024-06-02 15:00:00', '2024-06-02 15:30:00'),
('user020@naver.com', '088', '신한은행', 'ENC_100100010020', '사용자020', 'Y', '2024-06-06 10:00:00', '2024-06-06 10:30:00');

-- USER_CARD: 사용자 카드 정보 (20명)
INSERT INTO USER_CARD (
    USER_ID, BILLING_KEY, CARD_COMPANY, CARD_NUMBER, REG_DATE
) VALUES
('user001@gmail.com', 'BILLKEY_001', 'KB국민카드', '4012-****-****-0001', '2024-03-02 10:00:00'),
('user002@naver.com', 'BILLKEY_002', '신한카드', '5213-****-****-0002', '2024-03-06 11:00:00'),
('user003@daum.net', 'BILLKEY_003', '현대카드', '5412-****-****-0003', '2024-03-11 14:00:00'),
('user004@gmail.com', 'BILLKEY_004', '롯데카드', '5312-****-****-0004', '2024-03-16 09:00:00'),
('user005@naver.com', 'BILLKEY_005', '삼성카드', '5512-****-****-0005', '2024-03-21 16:00:00'),
('user006@daum.net', 'BILLKEY_006', 'KB국민카드', '4012-****-****-0006', '2024-03-26 13:00:00'),
('user007@gmail.com', 'BILLKEY_007', '신한카드', '5213-****-****-0007', '2024-04-02 10:00:00'),
('user008@naver.com', 'BILLKEY_008', '현대카드', '5412-****-****-0008', '2024-04-06 11:00:00'),
('user009@daum.net', 'BILLKEY_009', '롯데카드', '5312-****-****-0009', '2024-04-11 15:00:00'),
('user010@gmail.com', 'BILLKEY_010', '삼성카드', '5512-****-****-0010', '2024-04-16 09:00:00'),
('user011@naver.com', 'BILLKEY_011', 'KB국민카드', '4012-****-****-0011', '2024-04-21 14:00:00'),
('user012@daum.net', 'BILLKEY_012', '신한카드', '5213-****-****-0012', '2024-04-26 10:00:00'),
('user013@gmail.com', 'BILLKEY_013', '현대카드', '5412-****-****-0013', '2024-05-02 13:00:00'),
('user014@naver.com', 'BILLKEY_014', '롯데카드', '5312-****-****-0014', '2024-05-06 16:00:00'),
('user015@daum.net', 'BILLKEY_015', '삼성카드', '5512-****-****-0015', '2024-05-11 11:00:00'),
('user016@gmail.com', 'BILLKEY_016', 'KB국민카드', '4012-****-****-0016', '2024-05-16 14:00:00'),
('user017@naver.com', 'BILLKEY_017', '신한카드', '5213-****-****-0017', '2024-05-21 09:00:00'),
('user018@daum.net', 'BILLKEY_018', '현대카드', '5412-****-****-0018', '2024-05-26 12:00:00'),
('user019@gmail.com', 'BILLKEY_019', '롯데카드', '5312-****-****-0019', '2024-06-02 15:00:00'),
('user020@naver.com', 'BILLKEY_020', '삼성카드', '5512-****-****-0020', '2024-06-06 10:00:00');

-- SUBSCRIPTION: 구독 정보 (20명)
INSERT INTO SUBSCRIPTION (
    USER_ID, PRODUCT_ID, SUBSCRIPTION_STATUS,
    START_DATE, END_DATE, CANCEL_REASON, CANCEL_DATE
) VALUES
('user001@gmail.com', 1, 'ACTIVE', '2024-04-01', NULL, NULL, NULL),
('user002@naver.com', 1, 'ACTIVE', '2024-04-05', NULL, NULL, NULL),
('user003@daum.net', 2, 'ACTIVE', '2024-04-10', NULL, NULL, NULL),
('user004@gmail.com', 2, 'ACTIVE', '2024-04-15', NULL, NULL, NULL),
('user005@naver.com', 3, 'ACTIVE', '2024-04-20', NULL, NULL, NULL),
('user006@daum.net', 3, 'ACTIVE', '2024-04-25', NULL, NULL, NULL),
('user007@gmail.com', 4, 'ACTIVE', '2024-05-01', NULL, NULL, NULL),
('user008@naver.com', 4, 'ACTIVE', '2024-05-05', NULL, NULL, NULL),
('user009@daum.net', 5, 'ACTIVE', '2024-05-10', NULL, NULL, NULL),
('user010@gmail.com', 5, 'ACTIVE', '2024-05-15', NULL, NULL, NULL),
('user011@naver.com', 6, 'ACTIVE', '2024-05-20', NULL, NULL, NULL),
('user012@daum.net', 6, 'ACTIVE', '2024-05-25', NULL, NULL, NULL),
('user013@gmail.com', 7, 'ACTIVE', '2024-06-01', NULL, NULL, NULL),
('user014@naver.com', 7, 'ACTIVE', '2024-06-05', NULL, NULL, NULL),
('user015@daum.net', 8, 'ACTIVE', '2024-06-10', NULL, NULL, NULL),
('user016@gmail.com', 8, 'ACTIVE', '2024-06-15', NULL, NULL, NULL),
('user017@naver.com', 9, 'ACTIVE', '2024-06-20', NULL, NULL, NULL),
('user018@daum.net', 9, 'ACTIVE', '2024-06-25', NULL, NULL, NULL),
('user019@gmail.com', 10, 'ACTIVE', '2024-07-01', NULL, NULL, NULL),
('user020@naver.com', 10, 'ACTIVE', '2024-07-05', NULL, NULL, NULL);

-- ============================================
-- 5. 파티 데이터 (5개 파티, 각 4명)
-- ============================================

-- PARTY: 파티 5개
INSERT INTO PARTY (
    PARTY_ID, PRODUCT_ID, PARTY_LEADER_ID, PARTY_STATUS,
    MAX_MEMBERS, CURRENT_MEMBERS, MONTHLY_FEE,
    OTT_ID, OTT_PASSWORD, ACCOUNT_ID,
    REG_DATE, START_DATE, END_DATE
) VALUES
(1, 1, 'user001@gmail.com', 'ACTIVE', 4, 4, 4250, 'googleai_pro_001', 'googleai!001', 1, '2024-04-01 10:00:00', '2024-04-05 00:00:00', NULL),
(2, 2, 'user003@daum.net', 'ACTIVE', 4, 4, 2725, 'disney_plus_002', 'disney!002', 3, '2024-04-10 10:00:00', '2024-04-15 00:00:00', NULL),
(3, 3, 'user005@naver.com', 'ACTIVE', 4, 4, 1975, 'watcha_basic_003', 'watcha!003', 5, '2024-04-20 10:00:00', '2024-04-25 00:00:00', NULL),
(4, 4, 'user007@gmail.com', 'ACTIVE', 4, 4, 3475, 'youtube_premium_004', 'youtube!004', 7, '2024-05-01 10:00:00', '2024-05-05 00:00:00', NULL),
(5, 5, 'user009@daum.net', 'ACTIVE', 4, 4, 7250, 'chatgpt_plus_005', 'chatgpt!005', 9, '2024-05-10 10:00:00', '2024-05-15 00:00:00', NULL);

-- PARTY_MEMBER: 파티 멤버 20명 (5개 파티 × 4명)
INSERT INTO PARTY_MEMBER (
    PARTY_MEMBER_ID, PARTY_ID, USER_ID, MEMBER_ROLE, MEMBER_STATUS, JOIN_DATE, WITHDRAW_DATE
) VALUES
-- 파티 1 (Google AI Pro)
(1, 1, 'user001@gmail.com', 'LEADER', 'ACTIVE', '2024-04-01 10:00:00', NULL),
(2, 1, 'user002@naver.com', 'MEMBER', 'ACTIVE', '2024-04-02 10:00:00', NULL),
(3, 1, 'user011@naver.com', 'MEMBER', 'ACTIVE', '2024-04-03 10:00:00', NULL),
(4, 1, 'user012@daum.net', 'MEMBER', 'ACTIVE', '2024-04-04 10:00:00', NULL),

-- 파티 2 (디즈니+ 스탠다드)
(5, 2, 'user003@daum.net', 'LEADER', 'ACTIVE', '2024-04-10 10:00:00', NULL),
(6, 2, 'user004@gmail.com', 'MEMBER', 'ACTIVE', '2024-04-11 10:00:00', NULL),
(7, 2, 'user013@gmail.com', 'MEMBER', 'ACTIVE', '2024-04-12 10:00:00', NULL),
(8, 2, 'user014@naver.com', 'MEMBER', 'ACTIVE', '2024-04-13 10:00:00', NULL),

-- 파티 3 (왓챠 베이직)
(9, 3, 'user005@naver.com', 'LEADER', 'ACTIVE', '2024-04-20 10:00:00', NULL),
(10, 3, 'user006@daum.net', 'MEMBER', 'ACTIVE', '2024-04-21 10:00:00', NULL),
(11, 3, 'user015@daum.net', 'MEMBER', 'ACTIVE', '2024-04-22 10:00:00', NULL),
(12, 3, 'user016@gmail.com', 'MEMBER', 'ACTIVE', '2024-04-23 10:00:00', NULL),

-- 파티 4 (유튜브 프리미엄)
(13, 4, 'user007@gmail.com', 'LEADER', 'ACTIVE', '2024-05-01 10:00:00', NULL),
(14, 4, 'user008@naver.com', 'MEMBER', 'ACTIVE', '2024-05-02 10:00:00', NULL),
(15, 4, 'user017@naver.com', 'MEMBER', 'ACTIVE', '2024-05-03 10:00:00', NULL),
(16, 4, 'user018@daum.net', 'MEMBER', 'ACTIVE', '2024-05-04 10:00:00', NULL),

-- 파티 5 (챗GPT 플러스)
(17, 5, 'user009@daum.net', 'LEADER', 'ACTIVE', '2024-05-10 10:00:00', NULL),
(18, 5, 'user010@gmail.com', 'MEMBER', 'ACTIVE', '2024-05-11 10:00:00', NULL),
(19, 5, 'user019@gmail.com', 'MEMBER', 'ACTIVE', '2024-05-12 10:00:00', NULL),
(20, 5, 'user020@naver.com', 'MEMBER', 'ACTIVE', '2024-05-13 10:00:00', NULL);

-- ============================================
-- 6. 결제 데이터 (보증금 + 월회비)
-- ============================================

-- DEPOSIT: 보증금 20건
INSERT INTO DEPOSIT (
    DEPOSIT_ID, PARTY_ID, PARTY_MEMBER_ID, USER_ID, DEPOSIT_TYPE,
    DEPOSIT_AMOUNT, DEPOSIT_STATUS, PAYMENT_DATE, REFUND_DATE, REFUND_AMOUNT,
    TOSS_PAYMENT_KEY, ORDER_ID
) VALUES
(1, 1, 1, 'user001@gmail.com', 'LEADER', 17000, 'PAID', '2024-04-01 10:05:00', NULL, NULL, 'toss_dep_001', 'ORD_DEP_001'),
(2, 1, 2, 'user002@naver.com', 'SECURITY', 4250, 'PAID', '2024-04-02 10:05:00', NULL, NULL, 'toss_dep_002', 'ORD_DEP_002'),
(3, 1, 3, 'user011@naver.com', 'SECURITY', 4250, 'PAID', '2024-04-03 10:05:00', NULL, NULL, 'toss_dep_003', 'ORD_DEP_003'),
(4, 1, 4, 'user012@daum.net', 'SECURITY', 4250, 'PAID', '2024-04-04 10:05:00', NULL, NULL, 'toss_dep_004', 'ORD_DEP_004'),
(5, 2, 5, 'user003@daum.net', 'LEADER', 10900, 'PAID', '2024-04-10 10:05:00', NULL, NULL, 'toss_dep_005', 'ORD_DEP_005'),
(6, 2, 6, 'user004@gmail.com', 'SECURITY', 2725, 'PAID', '2024-04-11 10:05:00', NULL, NULL, 'toss_dep_006', 'ORD_DEP_006'),
(7, 2, 7, 'user013@gmail.com', 'SECURITY', 2725, 'PAID', '2024-04-12 10:05:00', NULL, NULL, 'toss_dep_007', 'ORD_DEP_007'),
(8, 2, 8, 'user014@naver.com', 'SECURITY', 2725, 'PAID', '2024-04-13 10:05:00', NULL, NULL, 'toss_dep_008', 'ORD_DEP_008'),
(9, 3, 9, 'user005@naver.com', 'LEADER', 7900, 'PAID', '2024-04-20 10:05:00', NULL, NULL, 'toss_dep_009', 'ORD_DEP_009'),
(10, 3, 10, 'user006@daum.net', 'SECURITY', 1975, 'PAID', '2024-04-21 10:05:00', NULL, NULL, 'toss_dep_010', 'ORD_DEP_010'),
(11, 3, 11, 'user015@daum.net', 'SECURITY', 1975, 'PAID', '2024-04-22 10:05:00', NULL, NULL, 'toss_dep_011', 'ORD_DEP_011'),
(12, 3, 12, 'user016@gmail.com', 'SECURITY', 1975, 'PAID', '2024-04-23 10:05:00', NULL, NULL, 'toss_dep_012', 'ORD_DEP_012'),
(13, 4, 13, 'user007@gmail.com', 'LEADER', 13900, 'PAID', '2024-05-01 10:05:00', NULL, NULL, 'toss_dep_013', 'ORD_DEP_013'),
(14, 4, 14, 'user008@naver.com', 'SECURITY', 3475, 'PAID', '2024-05-02 10:05:00', NULL, NULL, 'toss_dep_014', 'ORD_DEP_014'),
(15, 4, 15, 'user017@naver.com', 'SECURITY', 3475, 'PAID', '2024-05-03 10:05:00', NULL, NULL, 'toss_dep_015', 'ORD_DEP_015'),
(16, 4, 16, 'user018@daum.net', 'SECURITY', 3475, 'PAID', '2024-05-04 10:05:00', NULL, NULL, 'toss_dep_016', 'ORD_DEP_016'),
(17, 5, 17, 'user009@daum.net', 'LEADER', 29000, 'PAID', '2024-05-10 10:05:00', NULL, NULL, 'toss_dep_017', 'ORD_DEP_017'),
(18, 5, 18, 'user010@gmail.com', 'SECURITY', 7250, 'PAID', '2024-05-11 10:05:00', NULL, NULL, 'toss_dep_018', 'ORD_DEP_018'),
(19, 5, 19, 'user019@gmail.com', 'SECURITY', 7250, 'PAID', '2024-05-12 10:05:00', NULL, NULL, 'toss_dep_019', 'ORD_DEP_019'),
(20, 5, 20, 'user020@naver.com', 'SECURITY', 7250, 'PAID', '2024-05-13 10:05:00', NULL, NULL, 'toss_dep_020', 'ORD_DEP_020');

-- PAYMENT: 월회비 결제 20건
INSERT INTO PAYMENT (
    PAYMENT_ID, PARTY_ID, PARTY_MEMBER_ID, USER_ID, PAYMENT_TYPE,
    PAYMENT_AMOUNT, PAYMENT_STATUS, PAYMENT_METHOD,
    PAYMENT_DATE, TOSS_PAYMENT_KEY, ORDER_ID,
    CARD_NUMBER, CARD_COMPANY, TARGET_MONTH
) VALUES
(1, 1, 1, 'user001@gmail.com', 'MONTHLY', 4250, 'COMPLETED', 'CARD', '2024-05-01 00:05:00', 'toss_pk_001', 'ORD_202405_001', '4012-****-****-0001', 'KB국민카드', '2024-05'),
(2, 1, 2, 'user002@naver.com', 'MONTHLY', 4250, 'COMPLETED', 'CARD', '2024-05-01 00:05:00', 'toss_pk_002', 'ORD_202405_002', '5213-****-****-0002', '신한카드', '2024-05'),
(3, 1, 3, 'user011@naver.com', 'MONTHLY', 4250, 'COMPLETED', 'CARD', '2024-05-01 00:05:00', 'toss_pk_003', 'ORD_202405_003', '4012-****-****-0011', 'KB국민카드', '2024-05'),
(4, 1, 4, 'user012@daum.net', 'MONTHLY', 4250, 'COMPLETED', 'CARD', '2024-05-01 00:05:00', 'toss_pk_004', 'ORD_202405_004', '5213-****-****-0012', '신한카드', '2024-05'),
(5, 2, 5, 'user003@daum.net', 'MONTHLY', 2725, 'COMPLETED', 'CARD', '2024-05-15 00:05:00', 'toss_pk_005', 'ORD_202405_005', '5412-****-****-0003', '현대카드', '2024-05'),
(6, 2, 6, 'user004@gmail.com', 'MONTHLY', 2725, 'COMPLETED', 'CARD', '2024-05-15 00:05:00', 'toss_pk_006', 'ORD_202405_006', '5312-****-****-0004', '롯데카드', '2024-05'),
(7, 2, 7, 'user013@gmail.com', 'MONTHLY', 2725, 'COMPLETED', 'CARD', '2024-05-15 00:05:00', 'toss_pk_007', 'ORD_202405_007', '5412-****-****-0013', '현대카드', '2024-05'),
(8, 2, 8, 'user014@naver.com', 'MONTHLY', 2725, 'COMPLETED', 'CARD', '2024-05-15 00:05:00', 'toss_pk_008', 'ORD_202405_008', '5312-****-****-0014', '롯데카드', '2024-05'),
(9, 3, 9, 'user005@naver.com', 'MONTHLY', 1975, 'COMPLETED', 'CARD', '2024-05-25 00:05:00', 'toss_pk_009', 'ORD_202405_009', '5512-****-****-0005', '삼성카드', '2024-05'),
(10, 3, 10, 'user006@daum.net', 'MONTHLY', 1975, 'COMPLETED', 'CARD', '2024-05-25 00:05:00', 'toss_pk_010', 'ORD_202405_010', '4012-****-****-0006', 'KB국민카드', '2024-05'),
(11, 3, 11, 'user015@daum.net', 'MONTHLY', 1975, 'COMPLETED', 'CARD', '2024-05-25 00:05:00', 'toss_pk_011', 'ORD_202405_011', '5512-****-****-0015', '삼성카드', '2024-05'),
(12, 3, 12, 'user016@gmail.com', 'MONTHLY', 1975, 'COMPLETED', 'CARD', '2024-05-25 00:05:00', 'toss_pk_012', 'ORD_202405_012', '4012-****-****-0016', 'KB국민카드', '2024-05'),
(13, 4, 13, 'user007@gmail.com', 'MONTHLY', 3475, 'COMPLETED', 'CARD', '2024-06-05 00:05:00', 'toss_pk_013', 'ORD_202406_013', '5213-****-****-0007', '신한카드', '2024-06'),
(14, 4, 14, 'user008@naver.com', 'MONTHLY', 3475, 'COMPLETED', 'CARD', '2024-06-05 00:05:00', 'toss_pk_014', 'ORD_202406_014', '5412-****-****-0008', '현대카드', '2024-06'),
(15, 4, 15, 'user017@naver.com', 'MONTHLY', 3475, 'COMPLETED', 'CARD', '2024-06-05 00:05:00', 'toss_pk_015', 'ORD_202406_015', '5213-****-****-0017', '신한카드', '2024-06'),
(16, 4, 16, 'user018@daum.net', 'MONTHLY', 3475, 'COMPLETED', 'CARD', '2024-06-05 00:05:00', 'toss_pk_016', 'ORD_202406_016', '5412-****-****-0018', '현대카드', '2024-06'),
(17, 5, 17, 'user009@daum.net', 'MONTHLY', 7250, 'COMPLETED', 'CARD', '2024-06-15 00:05:00', 'toss_pk_017', 'ORD_202406_017', '5312-****-****-0009', '롯데카드', '2024-06'),
(18, 5, 18, 'user010@gmail.com', 'MONTHLY', 7250, 'COMPLETED', 'CARD', '2024-06-15 00:05:00', 'toss_pk_018', 'ORD_202406_018', '5512-****-****-0010', '삼성카드', '2024-06'),
(19, 5, 19, 'user019@gmail.com', 'MONTHLY', 7250, 'COMPLETED', 'CARD', '2024-06-15 00:05:00', 'toss_pk_019', 'ORD_202406_019', '5312-****-****-0019', '롯데카드', '2024-06'),
(20, 5, 20, 'user020@naver.com', 'MONTHLY', 7250, 'COMPLETED', 'CARD', '2024-06-15 00:05:00', 'toss_pk_020', 'ORD_202406_020', '5512-****-****-0020', '삼성카드', '2024-06');

-- PAYMENT_RETRY_HISTORY: 결제 재시도 이력 (다양한 시나리오)
INSERT INTO PAYMENT_RETRY_HISTORY (
    PAYMENT_ID, PARTY_ID, PARTY_MEMBER_ID, 
    ATTEMPT_NUMBER, ATTEMPT_DATE, RETRY_REASON, 
    RETRY_STATUS, NEXT_RETRY_DATE, ERROR_CODE, ERROR_MESSAGE
) VALUES
(1, 1, 1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, 'SUCCESS', NULL, NULL, NULL),
(2, 1, 2, 1, DATE_SUB(NOW(), INTERVAL 25 HOUR), NULL, 'FAILED', DATE_SUB(NOW(), INTERVAL 1 HOUR), 'INSUFFICIENT_FUNDS', '잔액이 부족합니다.'),
(2, 1, 2, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR), '잔액이 부족합니다.', 'SUCCESS', NULL, NULL, NULL),
(3, 1, 3, 1, DATE_SUB(NOW(), INTERVAL 73 HOUR), NULL, 'FAILED', DATE_SUB(NOW(), INTERVAL 49 HOUR), 'EXCEED_MAX_CARD_LIMIT', '카드 한도를 초과했습니다.'),
(3, 1, 3, 2, DATE_SUB(NOW(), INTERVAL 49 HOUR), '카드 한도를 초과했습니다.', 'FAILED', DATE_SUB(NOW(), INTERVAL 1 HOUR), 'EXCEED_MAX_CARD_LIMIT', '카드 한도를 초과했습니다.'),
(3, 1, 3, 3, DATE_SUB(NOW(), INTERVAL 1 HOUR), '카드 한도를 초과했습니다.', 'SUCCESS', NULL, NULL, NULL),
(4, 1, 4, 1, DATE_SUB(NOW(), INTERVAL 169 HOUR), NULL, 'FAILED', DATE_SUB(NOW(), INTERVAL 145 HOUR), 'CARD_SUSPENDED', '카드가 정지되었습니다.'),
(4, 1, 4, 2, DATE_SUB(NOW(), INTERVAL 145 HOUR), '카드가 정지되었습니다.', 'FAILED', DATE_SUB(NOW(), INTERVAL 97 HOUR), 'CARD_SUSPENDED', '카드가 정지되었습니다.'),
(4, 1, 4, 3, DATE_SUB(NOW(), INTERVAL 97 HOUR), '카드가 정지되었습니다.', 'FAILED', DATE_SUB(NOW(), INTERVAL 25 HOUR), 'CARD_SUSPENDED', '카드가 정지되었습니다.'),
(4, 1, 4, 4, DATE_SUB(NOW(), INTERVAL 25 HOUR), '카드가 정지되었습니다.', 'SUCCESS', NULL, NULL, NULL),
(5, 2, 5, 1, DATE_SUB(NOW(), INTERVAL 169 HOUR), NULL, 'FAILED', DATE_SUB(NOW(), INTERVAL 145 HOUR), 'CARD_LOST', '분실 신고된 카드입니다.'),
(5, 2, 5, 2, DATE_SUB(NOW(), INTERVAL 145 HOUR), '분실 신고된 카드입니다.', 'FAILED', DATE_SUB(NOW(), INTERVAL 97 HOUR), 'CARD_LOST', '분실 신고된 카드입니다.'),
(5, 2, 5, 3, DATE_SUB(NOW(), INTERVAL 97 HOUR), '분실 신고된 카드입니다.', 'FAILED', DATE_SUB(NOW(), INTERVAL 25 HOUR), 'CARD_LOST', '분실 신고된 카드입니다.'),
(5, 2, 5, 4, DATE_SUB(NOW(), INTERVAL 25 HOUR), '분실 신고된 카드입니다.', 'FAILED', NULL, 'CARD_LOST', '분실 신고된 카드입니다. (최종 실패)');

-- ============================================
-- 7. 정산 데이터
-- ============================================

-- SETTLEMENT: 정산 5건
INSERT INTO SETTLEMENT (
    SETTLEMENT_ID, PARTY_ID, PARTY_LEADER_ID, ACCOUNT_ID, 
    SETTLEMENT_MONTH, TOTAL_AMOUNT, COMMISSION_AMOUNT, NET_AMOUNT, 
    SETTLEMENT_STATUS, SETTLEMENT_DATE, BANK_TRAN_ID
) VALUES 
(1, 1, 'user001@gmail.com', 1, '2024-05', 17000, 2550, 14450, 'COMPLETED', '2024-06-05 10:00:00', 'T202406050001'),
(2, 2, 'user003@daum.net', 3, '2024-05', 10900, 1635, 9265, 'COMPLETED', '2024-06-05 10:30:00', 'T202406050002'),
(3, 3, 'user005@naver.com', 5, '2024-05', 7900, 1185, 6715, 'COMPLETED', '2024-06-05 11:00:00', 'T202406050003'),
(4, 4, 'user007@gmail.com', 7, '2024-06', 13900, 2085, 11815, 'COMPLETED', '2024-07-05 11:00:00', 'T202407050001'),
(5, 5, 'user009@daum.net', 9, '2024-06', 29000, 4350, 24650, 'COMPLETED', '2024-07-05 11:30:00', 'T202407050002');

-- REFUND_RETRY_HISTORY: 보증금 환불 재시도 이력
INSERT INTO REFUND_RETRY_HISTORY (
    DEPOSIT_ID, TOSS_PAYMENT_KEY, ATTEMPT_NUMBER, ATTEMPT_DATE, RETRY_STATUS, RETRY_TYPE,
    NEXT_RETRY_DATE, REFUND_AMOUNT, REFUND_REASON,
    ERROR_CODE, ERROR_MESSAGE
) VALUES
(2, 'toss_dep_002', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'SUCCESS', 'REFUND', NULL, 4250, '파티 정상 종료', NULL, NULL),
(3, 'toss_dep_003', 1, DATE_SUB(NOW(), INTERVAL 26 HOUR), 'FAILED', 'REFUND', DATE_SUB(NOW(), INTERVAL 2 HOUR), 4250, '파티 정상 종료', 'ALREADY_CANCELED', '이미 취소된 결제입니다.'),
(3, 'toss_dep_003', 2, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'SUCCESS', 'REFUND', NULL, 4250, '파티 정상 종료', NULL, NULL),
(4, 'toss_dep_004', 1, DATE_SUB(NOW(), INTERVAL 74 HOUR), 'FAILED', 'REFUND', DATE_SUB(NOW(), INTERVAL 50 HOUR), 4250, '중도 탈퇴 (50% 환불)', 'CANCEL_AMOUNT_EXCEED', '취소 가능 금액을 초과했습니다.'),
(4, 'toss_dep_004', 2, DATE_SUB(NOW(), INTERVAL 50 HOUR), 'FAILED', 'REFUND', DATE_SUB(NOW(), INTERVAL 2 HOUR), 2125, '중도 탈퇴 (50% 환불)', 'PAYMENT_NOT_FOUND', '결제 정보를 찾을 수 없습니다.'),
(4, 'toss_dep_004', 3, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'SUCCESS', 'REFUND', NULL, 2125, '중도 탈퇴 (50% 환불)', NULL, NULL),
(5, 'toss_dep_005', 1, DATE_SUB(NOW(), INTERVAL 26 HOUR), 'FAILED', 'REFUND', DATE_ADD(NOW(), INTERVAL 2 HOUR), 10900, '파티 정상 종료', 'TEMPORARY_ERROR', '일시적인 오류가 발생했습니다.'),
(6, 'toss_dep_006', 1, DATE_SUB(NOW(), INTERVAL 170 HOUR), 'FAILED', 'COMPENSATION', DATE_SUB(NOW(), INTERVAL 146 HOUR), 2725, '파티장 탈퇴', 'INVALID_PAYMENT_KEY', '유효하지 않은 결제 키입니다.'),
(6, 'toss_dep_006', 2, DATE_SUB(NOW(), INTERVAL 146 HOUR), 'FAILED', 'COMPENSATION', DATE_SUB(NOW(), INTERVAL 98 HOUR), 2725, '파티장 탈퇴', 'INVALID_PAYMENT_KEY', '유효하지 않은 결제 키입니다.'),
(6, 'toss_dep_006', 3, DATE_SUB(NOW(), INTERVAL 98 HOUR), 'FAILED', 'COMPENSATION', DATE_SUB(NOW(), INTERVAL 26 HOUR), 2725, '파티장 탈퇴', 'INVALID_PAYMENT_KEY', '유효하지 않은 결제 키입니다.'),
(6, 'toss_dep_006', 4, DATE_SUB(NOW(), INTERVAL 26 HOUR), 'FAILED', 'COMPENSATION', NULL, 2725, '파티장 탈퇴 (최종 실패)', 'INVALID_PAYMENT_KEY', '유효하지 않은 결제 키입니다.');

-- SETTLEMENT_RETRY_HISTORY: 정산 이체 재시도 이력
INSERT INTO SETTLEMENT_RETRY_HISTORY (
    SETTLEMENT_ID, ATTEMPT_NUMBER, ATTEMPT_DATE, RETRY_REASON, RETRY_STATUS,
    NEXT_RETRY_DATE, TRANSFER_AMOUNT,
    ERROR_CODE, ERROR_MESSAGE, BANK_RSP_CODE, BANK_RSP_MESSAGE, BANK_TRAN_ID
) VALUES
(1, 1, '2024-06-05 09:55:00', NULL, 'SUCCESS', NULL, 14450, NULL, NULL, '000', '정상처리', 'T202406050001'),
(2, 1, '2024-06-05 10:25:00', NULL, 'FAILED', '2024-06-05 12:25:00', 9265, 'A0003', '수취인 계좌 오류', '301', '수취계좌오류', NULL),
(2, 2, '2024-06-05 10:30:00', '수취인 계좌 오류', 'SUCCESS', NULL, 9265, NULL, NULL, '000', '정상처리', 'T202406050002'),
(3, 1, '2024-06-05 10:55:00', NULL, 'FAILED', '2024-06-05 12:55:00', 6715, 'A0005', '출금 한도 초과', '512', '출금한도초과', NULL),
(3, 2, '2024-06-05 12:55:00', '출금 한도 초과', 'FAILED', '2024-06-05 14:55:00', 6715, 'A0005', '출금 한도 초과', '512', '출금한도초과', NULL),
(3, 3, '2024-06-05 11:00:00', '출금 한도 초과', 'SUCCESS', NULL, 6715, NULL, NULL, '000', '정상처리', 'T202406050003'),
(4, 1, DATE_SUB(NOW(), INTERVAL 26 HOUR), NULL, 'FAILED', DATE_ADD(NOW(), INTERVAL 2 HOUR), 11815, 'A0007', '계좌 동결 상태', '560', '계좌동결상태', NULL),
(5, 1, '2024-07-05 11:25:00', NULL, 'FAILED', '2024-07-05 13:25:00', 24650, 'A0001', '계좌번호 없음', '115', '해당계좌없음', NULL),
(5, 2, '2024-07-05 13:25:00', '계좌번호 없음', 'FAILED', '2024-07-05 15:25:00', 24650, 'A0001', '계좌번호 없음', '115', '해당계좌없음', NULL),
(5, 3, '2024-07-05 15:25:00', '계좌번호 없음', 'FAILED', '2024-07-05 17:25:00', 24650, 'A0001', '계좌번호 없음', '115', '해당계좌없음', NULL),
(5, 4, '2024-07-05 17:25:00', '계좌번호 없음 (최종 실패)', 'FAILED', NULL, 24650, 'A0001', '계좌번호 없음 - 수동 처리 필요', '115', '해당계좌없음', NULL);

-- ============================================
-- 8. 게시판 데이터
-- ============================================

-- COMMUNITY: 공지사항 3건 + 문의 7건
INSERT INTO COMMUNITY (
    USER_ID, COMMUNITY_CODE_ID, TITLE, CONTENT,
    CREATED_AT, VIEW_COUNT, FILE_ORIGINAL, FILE_UUID,
    ANSWER_CONTENT, ANSWERED_AT, ANSWER_STATUS
) VALUES
('admin@moa.com', 10, '[공지-시스템] MOA 서비스 정식 오픈 안내', 'MOA OTT 구독 공유 서비스가 정식 오픈하였습니다.', '2024-04-01 09:00:00', 1523, NULL, NULL, NULL, NULL, NULL),
('admin@moa.com', 4, '[FAQ] 파티 가입 방법이 궁금해요', '원하는 OTT 서비스를 선택한 후 모집 중인 파티에 가입하세요.', '2024-04-01 10:00:00', 3421, NULL, NULL, NULL, NULL, NULL),
('admin@moa.com', 4, '[FAQ] 보증금은 언제 환불되나요?', '파티 정상 종료 시 보증금 전액이 환불됩니다.', '2024-04-01 10:30:00', 2876, NULL, NULL, NULL, NULL, NULL),
('user001@gmail.com', 1, '휴대폰 번호 변경 후 본인인증이 안돼요', '최근 휴대폰 번호를 변경했는데 본인인증이 계속 실패합니다.', '2024-11-25 09:30:00', NULL, NULL, NULL, '번호 변경 시 통신사 정보 업데이트에 최대 24시간이 소요될 수 있습니다.', '2024-11-25 14:00:00', '답변완료'),
('user002@naver.com', 1, '프로필 이미지 업로드가 안됩니다', '프로필 이미지를 변경하려고 하는데 계속 오류가 발생합니다.', '2024-11-26 10:15:00', NULL, 'profile_error.png', 'uuid_profile_001.png', '이미지 형식을 JPG 또는 PNG로 변경 후 다시 시도해주세요.', '2024-11-26 15:30:00', '답변완료'),
('user003@daum.net', 2, '이번 달 결제가 두 번 됐어요', '11월 1일에 결제가 끝났는데 11월 3일에 또 결제가 되었습니다.', '2024-11-05 08:30:00', NULL, 'payment_double.png', 'uuid_payment_001.png', '확인 결과 11월 1일 결제 실패 후 3일에 재결제가 진행되었습니다.', '2024-11-05 11:00:00', '답변완료'),
('user004@gmail.com', 3, '파티장이 계정 정보를 안 알려줘요', '파티에 가입했는데 파티장이 OTT 계정 정보를 공유하지 않습니다.', '2024-11-08 09:00:00', NULL, NULL, NULL, '파티장에게 알림을 발송하였습니다.', '2024-11-08 10:30:00', '답변완료'),
('user005@naver.com', 3, '파티원이 비밀번호를 변경했어요', '파티원 중 한 명이 OTT 비밀번호를 임의로 변경해서 접속이 안 됩니다.', '2024-11-12 14:00:00', NULL, NULL, NULL, '해당 파티원에게 경고 조치하였습니다.', '2024-11-12 16:00:00', '답변완료'),
('user006@daum.net', 3, '이번 달 정산금이 입금되지 않았어요', '매월 5일에 정산금이 들어오는데 오늘 10일인데도 입금이 안 됐습니다.', '2024-11-10 09:00:00', NULL, NULL, NULL, '확인 결과 계좌번호 오류로 입금이 반려되었습니다.', '2024-11-10 11:00:00', '답변완료'),
('user007@gmail.com', 3, '앱이 자꾸 튕겨요', '앱을 실행하면 메인 화면에서 계속 튕깁니다.', '2024-11-15 09:00:00', NULL, NULL, NULL, '앱 삭제 후 재설치를 시도해주세요.', '2024-11-15 11:30:00', '답변완료');

-- ============================================
-- 9. 푸시 알림 데이터
-- ============================================

-- PUSH: 푸시 알림 10건
INSERT INTO PUSH (
    RECEIVER_ID, PUSH_CODE, TITLE, CONTENT,
    MODULE_ID, MODULE_TYPE, SENT_AT, READ_AT, IS_READ, IS_DELETED
) VALUES
('user001@gmail.com', 'PAYMENT_SUCCESS', '결제 완료', '사용자001님의 Google AI Pro 파티 월회비 4,250원 결제가 완료되었습니다.', '1', 'PAYMENT', '2024-05-01 00:05:00', '2024-05-01 08:30:00', 'Y', 'N'),
('user002@naver.com', 'PAYMENT_SUCCESS', '결제 완료', '사용자002님의 Google AI Pro 파티 월회비 4,250원 결제가 완료되었습니다.', '2', 'PAYMENT', '2024-05-01 00:05:00', '2024-05-01 09:00:00', 'Y', 'N'),
('user003@daum.net', 'PAYMENT_SUCCESS', '결제 완료', '사용자003님의 디즈니+ 스탠다드 파티 월회비 2,725원 결제가 완료되었습니다.', '5', 'PAYMENT', '2024-05-15 00:05:00', NULL, 'N', 'N'),
('user001@gmail.com', 'PARTY_START', '파티 시작', 'Google AI Pro 파티가 시작되었습니다. OTT 계정 정보를 확인하세요.', '1', 'PARTY', '2024-04-05 00:00:00', '2024-04-05 07:00:00', 'Y', 'N'),
('user003@daum.net', 'PARTY_START', '파티 시작', '디즈니+ 스탠다드 파티가 시작되었습니다. OTT 계정 정보를 확인하세요.', '2', 'PARTY', '2024-04-15 00:00:00', '2024-04-15 08:30:00', 'Y', 'N'),
('user002@naver.com', 'PARTY_JOIN', '파티 가입 완료', '사용자002님, Google AI Pro 파티에 성공적으로 참여하셨습니다.', '1', 'PARTY', '2024-04-02 14:25:00', '2024-04-02 15:00:00', 'Y', 'N'),
('user004@gmail.com', 'PARTY_JOIN', '파티 가입 완료', '사용자004님, 디즈니+ 스탠다드 파티에 성공적으로 참여하셨습니다.', '2', 'PARTY', '2024-04-11 10:05:00', '2024-04-11 11:00:00', 'Y', 'N'),
('user001@gmail.com', 'SETTLEMENT_MONTHLY', '월간 정산 완료', '5월 정산 금액 14,450원이 입금될 예정입니다.', '1', 'SETTLEMENT', '2024-06-05 10:00:00', '2024-06-05 11:00:00', 'Y', 'N'),
('user003@daum.net', 'SETTLEMENT_MONTHLY', '월간 정산 완료', '5월 정산 금액 9,265원이 입금될 예정입니다.', '2', 'SETTLEMENT', '2024-06-05 10:30:00', '2024-06-05 12:00:00', 'Y', 'N'),
('user001@gmail.com', 'INQUIRY_ANSWER', '문의 답변 완료', '사용자001님이 남기신 문의에 답변이 등록되었습니다.', '4', 'COMMUNITY', '2024-11-25 14:00:00', '2024-11-25 15:00:00', 'Y', 'N');

-- ============================================
-- 10. 계좌 인증 및 이체 거래 데이터
-- ============================================

-- ACCOUNT_VERIFICATION: 1원 인증 세션 (다양한 상태)
INSERT INTO ACCOUNT_VERIFICATION (
    USER_ID, BANK_TRAN_ID, BANK_CODE, ACCOUNT_NUM, ACCOUNT_HOLDER,
    VERIFY_CODE, ATTEMPT_COUNT, STATUS, EXPIRED_AT, CREATED_AT
) VALUES
('user001@gmail.com', 'T202412091001', '004', '100100010001', '사용자001', '1234', 1, 'VERIFIED', DATE_ADD(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
('user003@daum.net', 'T202412091002', '020', '100100010003', '사용자003', '5678', 1, 'VERIFIED', DATE_ADD(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
('user005@naver.com', 'T202412091003', '003', '100100010005', '사용자005', '9012', 1, 'VERIFIED', DATE_ADD(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 6 MINUTE)),
('user007@gmail.com', 'T202412091004', '004', '100100010007', '사용자007', '3456', 0, 'PENDING', DATE_ADD(NOW(), INTERVAL 3 MINUTE), NOW()),
('user009@daum.net', 'T202412091005', '020', '100100010009', '사용자009', '7890', 0, 'PENDING', DATE_ADD(NOW(), INTERVAL 4 MINUTE), NOW()),
('user011@naver.com', 'T202412091006', '003', '100100010011', '사용자011', '2345', 3, 'FAILED', DATE_ADD(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE)),
('user013@gmail.com', 'T202412091007', '004', '100100010013', '사용자013', '6789', 0, 'EXPIRED', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- TRANSFER_TRANSACTION: 입금이체 거래 기록
INSERT INTO TRANSFER_TRANSACTION (
    SETTLEMENT_ID, BANK_TRAN_ID, FINTECH_USE_NUM, TRAN_AMT,
    PRINT_CONTENT, REQ_CLIENT_NAME, RSP_CODE, RSP_MESSAGE, STATUS, CREATED_AT
) VALUES
(1, 'T202406050001', '100000000001', 14450, 'MOA정산금', '사용자001', '000', '정상처리', 'SUCCESS', '2024-06-05 10:00:00'),
(2, 'T202406050002', '100000000003', 9265, 'MOA정산금', '사용자003', '000', '정상처리', 'SUCCESS', '2024-06-05 10:30:00'),
(3, 'T202406050003', '100000000005', 6715, 'MOA정산금', '사용자005', '000', '정상처리', 'SUCCESS', '2024-06-05 11:00:00'),
(4, NULL, '100000000007', 11815, 'MOA정산금', '사용자007', NULL, NULL, 'PENDING', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, NULL, '100000000009', 24650, 'MOA정산금', '사용자009', '115', '해당계좌없음', 'FAILED', '2024-07-05 17:25:00');

-- ============================================
-- 샘플 데이터 입력 완료
-- ============================================

SET FOREIGN_KEY_CHECKS = 1;