-- ============================================
-- OTT 구독 공유 서비스 MOA 샘플 데이터
-- Version: 5.1 (데이터 무결성 개선 + 최적화)
-- 작성일: 2025.12.11
-- 변경사항:
--   - PUSH_CODE: 푸시 알림 템플릿 추가
--     ㄴ커뮤니티(1)/파티(6)/결제(10)/보증금(3)/정산(3)/오픈뱅킹(4)

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
-- ===== 커뮤니티 알림 (1개) =====
INSERT INTO PUSH_CODE (CODE_NAME, TITLE_TEMPLATE, CONTENT_TEMPLATE) VALUES
('INQUIRY_ANSWER', '문의 답변 완료', '{nickname}님이 남기신 문의에 답변이 등록되었습니다.');
-- ===== 파티 알림 (6개) =====
INSERT INTO PUSH_CODE (CODE_NAME, TITLE_TEMPLATE, CONTENT_TEMPLATE) VALUES
('PARTY_JOIN', '파티 가입 완료', '{nickname}님, {productName} 파티에 성공적으로 참여하셨습니다. (현재 {currentCount}/{maxCount}명)'),
('PARTY_WITHDRAW', '파티 탈퇴 완료', '{nickname}님, {productName} 파티에서 탈퇴 처리되었습니다.'),
('PARTY_START', '파티 시작', '{productName} 파티가 시작되었습니다. OTT 계정 정보를 확인하세요.'),
('PARTY_CLOSED', '파티 종료', '{productName} 파티가 종료되었습니다. 보증금이 환불됩니다.'),
('PARTY_MEMBER_JOIN', '새 파티원 참여', '{nickname}님이 {productName} 파티에 참여했습니다. (현재 {currentCount}/{maxCount}명)'),
('PARTY_MEMBER_WITHDRAW', '파티원 탈퇴', '{nickname}님이 {productName} 파티에서 탈퇴했습니다. (현재 {currentCount}/{maxCount}명)');
-- ===== 결제 알림 (10개) =====
INSERT INTO PUSH_CODE (CODE_NAME, TITLE_TEMPLATE, CONTENT_TEMPLATE) VALUES
('PAY_UPCOMING', '결제 예정 안내', '{productName} 파티 구독료 {amount}원이 내일({paymentDate}) 결제됩니다.'),
('PAY_SUCCESS', '결제 완료', '{productName} {targetMonth} 구독료 {amount}원이 결제되었습니다.'),
('PAY_FAILED_RETRY', '결제 실패 ({attemptNumber}차 시도)', '{productName} 결제 실패: {errorMessage}\n다음 재시도: {nextRetryDate}'),
('PAY_FAILED_BALANCE', '결제 실패 - 잔액 부족', '{productName} 결제 실패: 카드 잔액이 부족합니다.\n{nextRetryDate}까지 충전 후 자동 재시도됩니다.'),
('PAY_FAILED_LIMIT', '결제 실패 - 한도 초과', '{productName} 결제 실패: 카드 한도가 초과되었습니다.\n{nextRetryDate}에 재시도됩니다.'),
('PAY_FAILED_CARD', '결제 실패 - 카드 오류', '{productName} 결제 실패: 카드 정보를 확인해주세요.\n{nextRetryDate}까지 카드 변경이 필요합니다.'),
('PAY_FINAL_FAILED', '결제 최종 실패', '{productName} 결제가 {attemptNumber}회 모두 실패했습니다.\n사유: {errorMessage}\n3일 내 미결제 시 파티에서 제외됩니다.'),
('PAY_MEMBER_FAILED_LEADER', '파티원 결제 실패', '{memberNickname}님의 {productName} 결제가 최종 실패했습니다.\n사유: {errorMessage}'),
('PAY_RETRY_SUCCESS', '결제 재시도 성공', '{productName} 결제가 {attemptNumber}차 시도 만에 성공했습니다. ({amount}원)'),
('PAY_TIMEOUT', '파티 생성 취소', '결제 대기 시간(30분)이 초과되어 {productName} 파티 생성이 취소되었습니다.');
-- ===== 보증금 알림 (3개) =====
INSERT INTO PUSH_CODE (CODE_NAME, TITLE_TEMPLATE, CONTENT_TEMPLATE) VALUES
('DEPOSIT_REFUNDED', '보증금 환불 완료', '{productName} 파티 보증금 {amount}원이 환불되었습니다.'),
('DEPOSIT_FORFEITED', '보증금 몰수 안내', '{productName} 파티 보증금 {amount}원이 정책에 따라 몰수되었습니다.'),
('REFUND_SUCCESS', '환불 처리 완료', '{productName} 보증금 {amount}원 환불이 완료되었습니다.');
-- ===== 정산 알림 (3개) =====
INSERT INTO PUSH_CODE (CODE_NAME, TITLE_TEMPLATE, CONTENT_TEMPLATE) VALUES
('SETTLE_COMPLETED', '정산 입금 완료', '{settlementMonth} 정산금 {netAmount}원이 입금되었습니다.'),
('SETTLE_FAILED', '정산 실패', '{settlementMonth} 정산 이체에 실패했습니다. 계좌 정보를 확인해주세요.'),
('ACCOUNT_REQUIRED', '계좌 등록 필요', '정산을 받으시려면 계좌를 등록해주세요.');
-- ===== 오픈뱅킹 알림 (4개) =====
INSERT INTO PUSH_CODE (CODE_NAME, TITLE_TEMPLATE, CONTENT_TEMPLATE) VALUES
('VERIFY_REQUESTED', '1원 인증 요청', '계좌 인증을 위해 1원이 입금되었습니다. 입금자명을 확인해주세요.'),
('ACCOUNT_VERIFIED', '계좌 등록 완료', '계좌 인증이 완료되었습니다. 이제 정산을 받으실 수 있습니다.'),
('VERIFY_EXPIRED', '인증 만료', '계좌 인증 시간(5분)이 만료되었습니다. 다시 시도해주세요.'),
('VERIFY_EXCEEDED', '인증 시도 초과', '인증 시도 횟수(3회)를 초과했습니다. 다시 시도해주세요.');


-- CATEGORY: 상품 카테고리
INSERT INTO CATEGORY (CATEGORY_ID, CATEGORY_NAME) VALUES
(1, 'AI'),
(2, 'MEDIA'),
(3, 'EDU'),
(4, 'MEMBER');


INSERT INTO USERS (
    USER_ID, PASSWORD, NICKNAME, PHONE,
    PROFILE_IMAGE, ROLE, USER_STATUS, REG_DATE,
    CI, PASS_CERTIFIED_AT, LAST_LOGIN_DATE,
    LOGIN_FAIL_COUNT, UNLOCK_SCHEDULED_AT,
    DELETE_DATE, DELETE_TYPE, DELETE_DETAIL,
    AGREE_MARKETING, PROVIDER, OTP_SECRET, OTP_ENABLED
) VALUES(    
	'admin@moa.com',
    '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
    '관리자',
    '01099999999',
    '/img/profile/admin.png',
    'ADMIN',
    'ACTIVE',
    '2024-01-01 09:00:00',
    'CI_ADMIN_MOA_001',
    '2024-01-01 09:00:00',
    '2024-12-01 09:00:00',
    0,
    NULL,
    NULL,
    NULL,
    NULL,
    0,
    'LOCAL',
    NULL,
    0
),
(
 'shakkoum@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_ww8nqg',
 '01055962724',
 NULL, 'USER', 'ACTIVE',
 '2024-03-02 13:39:00',
 'CI_6lf070xgiwf5',
 '2024-03-02 13:39:00',
 '2024-03-04 13:39:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'jo3hz8jz@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_bdbris',
 '01065660765',
 NULL, 'USER', 'ACTIVE',
 '2024-03-03 14:46:00',
 'CI_jyk0o29hn27k',
 '2024-03-03 14:46:00',
 '2024-03-23 14:46:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'vw51kceh@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_o3t4tq',
 '01070808021',
 NULL, 'USER', 'ACTIVE',
 '2024-03-04 15:22:00',
 'CI_cuoq690eupbe',
 '2024-03-04 15:22:00',
 '2024-06-12 15:22:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 '6etecsvu@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_wu3yro',
 '01035926815',
 NULL, 'USER', 'ACTIVE',
 '2024-03-05 13:46:00',
 'CI_9ygaty7wynvq',
 '2024-03-05 13:46:00',
 '2024-06-06 13:46:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'f92ii39x@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_hpdkxu',
 '01092345496',
 NULL, 'USER', 'ACTIVE',
 '2024-03-06 11:58:00',
 'CI_97bwdq6ercpe',
 '2024-03-06 11:58:00',
 '2024-07-27 11:58:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 '681vf6nk@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_oiwjsk',
 '01041811144',
 NULL, 'USER', 'ACTIVE',
 '2024-03-07 14:13:00',
 'CI_0a6wqhfse06i',
 '2024-03-07 14:13:00',
 '2024-04-04 14:13:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 '7slvebq6@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_9ft98t',
 '01040716362',
 NULL, 'USER', 'ACTIVE',
 '2024-03-08 11:06:00',
 'CI_2zvsdvhv266c',
 '2024-03-08 11:06:00',
 '2024-05-14 11:06:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'jlkc678n@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_qfjeaa',
 '01096775907',
 NULL, 'USER', 'ACTIVE',
 '2024-03-09 15:35:00',
 'CI_ss9dm5sjnfh4',
 '2024-03-09 15:35:00',
 '2024-06-12 15:35:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'j7ixhpnb@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_zbdx73',
 '01049396054',
 NULL, 'USER', 'ACTIVE',
 '2024-03-10 12:01:00',
 'CI_ch9a9vesohbt',
 '2024-03-10 12:01:00',
 '2024-06-12 12:01:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 '169o4kxq@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_q248os',
 '01090200039',
 NULL, 'USER', 'ACTIVE',
 '2024-03-11 17:57:00',
 'CI_twbg1hsmxjnz',
 '2024-03-11 17:57:00',
 '2024-08-05 17:57:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'dj6mbd5o@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_lhs17m',
 '01016873691',
 NULL, 'USER', 'ACTIVE',
 '2024-03-12 09:58:00',
 'CI_8naus27gf5ez',
 '2024-03-12 09:58:00',
 '2024-04-23 09:58:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'ytway0kx@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_0f1du7',
 '01071425909',
 NULL, 'USER', 'ACTIVE',
 '2024-03-13 10:24:00',
 'CI_kx7xk3m49rnd',
 '2024-03-13 10:24:00',
 '2024-07-09 10:24:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 '0spo51ch@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_g9n8o0',
 '01031965561',
 NULL, 'USER', 'ACTIVE',
 '2024-03-14 16:55:00',
 'CI_eyusbs9tjafc',
 '2024-03-14 16:55:00',
 '2024-04-28 16:55:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 's06x8nic@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_76taa4',
 '01069399599',
 NULL, 'USER', 'ACTIVE',
 '2024-03-15 16:22:00',
 'CI_byorlsjupm51',
 '2024-03-15 16:22:00',
 '2024-05-17 16:22:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 '6v1vjnub@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_nelqnd',
 '01046092605',
 NULL, 'USER', 'ACTIVE',
 '2024-03-16 14:37:00',
 'CI_jkp5mjq9pjrr',
 '2024-03-16 14:37:00',
 '2024-03-18 14:37:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'hb7thi3q@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_on89lq',
 '01049541271',
 NULL, 'USER', 'ACTIVE',
 '2024-03-17 17:38:00',
 'CI_anutimfh8fjo',
 '2024-03-17 17:38:00',
 '2024-09-30 17:38:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'mj40lbjs@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_0sak6m',
 '01081503282',
 NULL, 'USER', 'ACTIVE',
 '2024-03-18 16:01:00',
 'CI_14z7i2g5ca43',
 '2024-03-18 16:01:00',
 '2024-10-03 16:01:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 '7u3uf22s@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_c3zuwe',
 '01047310082',
 NULL, 'USER', 'ACTIVE',
 '2024-03-19 16:38:00',
 'CI_qb9vuuimcovu',
 '2024-03-19 16:38:00',
 '2024-08-28 16:38:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'ymh0evgm@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_jeublh',
 '01013148282',
 NULL, 'USER', 'ACTIVE',
 '2024-03-20 11:09:00',
 'CI_jwgpm2drtgj7',
 '2024-03-20 11:09:00',
 '2024-07-12 11:09:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'y417brpe@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_9f5zbz',
 '01043100815',
 NULL, 'USER', 'ACTIVE',
 '2024-03-21 18:42:00',
 'CI_4af1ayl02y1r',
 '2024-03-21 18:42:00',
 '2024-10-02 18:42:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 '8bc12k51@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_rcd60p',
 '01064460724',
 NULL, 'USER', 'ACTIVE',
 '2024-03-22 12:33:00',
 'CI_j2qexa2ens8z',
 '2024-03-22 12:33:00',
 '2024-06-20 12:33:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 '7tm3b8x3@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_di9lee',
 '01022673887',
 NULL, 'USER', 'ACTIVE',
 '2024-03-23 14:38:00',
 'CI_ifviom90kgdx',
 '2024-03-23 14:38:00',
 '2024-04-19 14:38:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'rnythef3@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_614awn',
 '01012103425',
 NULL, 'USER', 'ACTIVE',
 '2024-03-24 14:30:00',
 'CI_c2r3bvgq66o6',
 '2024-03-24 14:30:00',
 '2024-05-11 14:30:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 '00o3ymsd@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_b9pz97',
 '01091455761',
 NULL, 'USER', 'ACTIVE',
 '2024-03-25 11:55:00',
 'CI_i3xmuz8d8cwe',
 '2024-03-25 11:55:00',
 '2024-06-29 11:55:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 '3xhos8lm@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_neh7bm',
 '01064897537',
 NULL, 'USER', 'ACTIVE',
 '2024-03-26 10:10:00',
 'CI_beyco0yxe05g',
 '2024-03-26 10:10:00',
 '2024-06-10 10:10:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'um5uz5oh@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_bkg2th',
 '01010546758',
 NULL, 'USER', 'ACTIVE',
 '2024-03-27 11:59:00',
 'CI_725j2hcgsecq',
 '2024-03-27 11:59:00',
 '2024-07-03 11:59:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'g7jodbei@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_iaawmn',
 '01032007635',
 NULL, 'USER', 'ACTIVE',
 '2024-03-28 13:59:00',
 'CI_ebb8gubl3chv',
 '2024-03-28 13:59:00',
 '2024-09-01 13:59:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 '6b3lkw4i@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_h6ipw0',
 '01024763294',
 NULL, 'USER', 'ACTIVE',
 '2024-03-29 17:56:00',
 'CI_kj3rebw9uczm',
 '2024-03-29 17:56:00',
 '2024-06-11 17:56:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 '6ia3mj8s@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_bqpaed',
 '01078193944',
 NULL, 'USER', 'ACTIVE',
 '2024-03-30 09:15:00',
 'CI_ekzrop9u52np',
 '2024-03-30 09:15:00',
 '2024-10-13 09:15:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'rus0oscl@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_5wv682',
 '01057420216',
 NULL, 'USER', 'ACTIVE',
 '2024-03-31 15:21:00',
 'CI_3fzm1rkcpb02',
 '2024-03-31 15:21:00',
 '2024-08-14 15:21:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'd9w15ju4@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_viwsyi',
 '01090771952',
 NULL, 'USER', 'ACTIVE',
 '2024-04-01 12:48:00',
 'CI_oz0v2gjt9zzq',
 '2024-04-01 12:48:00',
 '2024-04-26 12:48:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'xkfegc4q@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_09wyo1',
 '01057082165',
 NULL, 'USER', 'ACTIVE',
 '2024-04-02 14:38:00',
 'CI_g9e5r64gh2fm',
 '2024-04-02 14:38:00',
 '2024-09-29 14:38:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 '12ozwk0a@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_4p2ouc',
 '01056090106',
 NULL, 'USER', 'ACTIVE',
 '2024-04-03 16:13:00',
 'CI_fpw6o02itjlc',
 '2024-04-03 16:13:00',
 '2024-08-28 16:13:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'sgwgqa8o@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_wbbcga',
 '01033636788',
 NULL, 'USER', 'ACTIVE',
 '2024-04-04 18:59:00',
 'CI_xqicg68ding9',
 '2024-04-04 18:59:00',
 '2024-08-15 18:59:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'rquh2xbl@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_8b3azy',
 '01026101670',
 NULL, 'USER', 'ACTIVE',
 '2024-04-05 11:43:00',
 'CI_lp5wpwxbq54i',
 '2024-04-05 11:43:00',
 '2024-10-20 11:43:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'jvm838og@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_j6aoo3',
 '01018856477',
 NULL, 'USER', 'ACTIVE',
 '2024-04-06 11:35:00',
 'CI_uh7wf9w4vnjc',
 '2024-04-06 11:35:00',
 '2024-05-24 11:35:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 '82unf1ln@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_z30203',
 '01082282716',
 NULL, 'USER', 'ACTIVE',
 '2024-04-07 09:01:00',
 'CI_g9fgqqdqzzh6',
 '2024-04-07 09:01:00',
 '2024-08-20 09:01:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'psjc2xtc@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_88tg8v',
 '01043780806',
 NULL, 'USER', 'ACTIVE',
 '2024-04-08 18:52:00',
 'CI_cqlslppsd0uh',
 '2024-04-08 18:52:00',
 '2024-06-26 18:52:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'dpnbe0js@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_x7lanl',
 '01054412479',
 NULL, 'USER', 'ACTIVE',
 '2024-04-09 15:52:00',
 'CI_irtrwxuyefpd',
 '2024-04-09 15:52:00',
 '2024-10-23 15:52:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'xowgxp8a@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_8hght3',
 '01025145901',
 NULL, 'USER', 'ACTIVE',
 '2024-04-10 14:14:00',
 'CI_tgbmb68w283i',
 '2024-04-10 14:14:00',
 '2024-07-27 14:14:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'xdg2da3e@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_9ubvd2',
 '01069977497',
 NULL, 'USER', 'ACTIVE',
 '2024-04-11 11:17:00',
 'CI_3mheg2fletr9',
 '2024-04-11 11:17:00',
 '2024-04-21 11:17:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'x8r4q9wa@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_h4dxpp',
 '01095463081',
 NULL, 'USER', 'ACTIVE',
 '2024-04-12 16:18:00',
 'CI_2o8eqkyqplqq',
 '2024-04-12 16:18:00',
 '2024-08-27 16:18:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'wsx5h9lm@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_rtbd9d',
 '01018267606',
 NULL, 'USER', 'ACTIVE',
 '2024-04-13 11:17:00',
 'CI_5butajhayo5f',
 '2024-04-13 11:17:00',
 '2024-08-02 11:17:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'psldbncn@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_cdqtor',
 '01036011701',
 NULL, 'USER', 'ACTIVE',
 '2024-04-14 12:58:00',
 'CI_thhlze101qcu',
 '2024-04-14 12:58:00',
 '2024-10-25 12:58:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 '9ca6gwtg@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_b48py2',
 '01057114328',
 NULL, 'USER', 'ACTIVE',
 '2024-04-15 17:39:00',
 'CI_lgrdph411zhv',
 '2024-04-15 17:39:00',
 '2024-06-22 17:39:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 '78yxorki@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_m2yk08',
 '01094303059',
 NULL, 'USER', 'ACTIVE',
 '2024-04-16 09:34:00',
 'CI_adsd81x9rn20',
 '2024-04-16 09:34:00',
 '2024-07-22 09:34:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'rfzrrhyx@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_rzt2xe',
 '01097574716',
 NULL, 'USER', 'ACTIVE',
 '2024-04-17 18:14:00',
 'CI_fl2y0x17aqdu',
 '2024-04-17 18:14:00',
 '2024-08-05 18:14:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'rbant52g@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_rcv4df',
 '01089955750',
 NULL, 'USER', 'ACTIVE',
 '2024-04-18 13:59:00',
 'CI_tqzmdi6ljj86',
 '2024-04-18 13:59:00',
 '2024-10-26 13:59:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'ofbdmzgk@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_q769l1',
 '01082451365',
 NULL, 'USER', 'ACTIVE',
 '2024-04-19 17:10:00',
 'CI_099mw3egl0ve',
 '2024-04-19 17:10:00',
 '2024-07-24 17:10:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 '5mphysdg@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_wi0i7d',
 '01079605068',
 NULL, 'USER', 'ACTIVE',
 '2024-04-20 15:58:00',
 'CI_vluz3xozpv8c',
 '2024-04-20 15:58:00',
 '2024-11-02 15:58:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'v9qj5nca@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_s83tlb',
 '01071665201',
 NULL, 'USER', 'ACTIVE',
 '2024-04-21 16:19:00',
 'CI_v8i4j7p8q2hr',
 '2024-04-21 16:19:00',
 '2024-04-29 16:19:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 '59gw81xb@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_7xl5zq',
 '01044649318',
 NULL, 'USER', 'ACTIVE',
 '2024-04-22 12:09:00',
 'CI_jxfp49rb1vbo',
 '2024-04-22 12:09:00',
 '2024-06-16 12:09:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'twsu1t42@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_dxw4vj',
 '01022309888',
 NULL, 'USER', 'ACTIVE',
 '2024-04-23 09:25:00',
 'CI_nori6sp6ifat',
 '2024-04-23 09:25:00',
 '2024-05-31 09:25:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'n39n1bih@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_md3a3t',
 '01065801619',
 NULL, 'USER', 'ACTIVE',
 '2024-04-24 18:42:00',
 'CI_wor8inz7z6q3',
 '2024-04-24 18:42:00',
 '2024-05-27 18:42:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'ke52g0y9@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_mbxzax',
 '01097831859',
 NULL, 'USER', 'ACTIVE',
 '2024-04-25 18:12:00',
 'CI_iab5meeuzwqr',
 '2024-04-25 18:12:00',
 '2024-05-07 18:12:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'r9ubfwga@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_4tc013',
 '01069545332',
 NULL, 'USER', 'ACTIVE',
 '2024-04-26 18:08:00',
 'CI_gz3b3id9c934',
 '2024-04-26 18:08:00',
 '2024-11-06 18:08:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'ljhy8ppb@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_5erlry',
 '01096728426',
 NULL, 'USER', 'ACTIVE',
 '2024-04-27 16:01:00',
 'CI_vpqlubkye15a',
 '2024-04-27 16:01:00',
 '2024-11-05 16:01:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'auug0tfd@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_56100s',
 '01046493097',
 NULL, 'USER', 'ACTIVE',
 '2024-04-28 15:55:00',
 'CI_e95j9nfdi8gt',
 '2024-04-28 15:55:00',
 '2024-10-14 15:55:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 't51r8w9n@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_ktcwga',
 '01088051397',
 NULL, 'USER', 'ACTIVE',
 '2024-04-29 10:19:00',
 'CI_uvsx5h7owpcp',
 '2024-04-29 10:19:00',
 '2024-07-28 10:19:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'zv1t25s2@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_l43if3',
 '01074100433',
 NULL, 'USER', 'ACTIVE',
 '2024-04-30 16:51:00',
 'CI_b7nvraz1jawz',
 '2024-04-30 16:51:00',
 '2024-08-11 16:51:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'n53hwoyy@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_hxisad',
 '01053781603',
 NULL, 'USER', 'ACTIVE',
 '2024-05-01 17:29:00',
 'CI_3lmx1gokcxhh',
 '2024-05-01 17:29:00',
 '2024-09-17 17:29:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'xw7b4au6@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_cm5ygt',
 '01053733901',
 NULL, 'USER', 'ACTIVE',
 '2024-05-02 11:05:00',
 'CI_hdsslr0b6cnh',
 '2024-05-02 11:05:00',
 '2024-06-30 11:05:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'za9bgykn@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_2gbiej',
 '01073461036',
 NULL, 'USER', 'ACTIVE',
 '2024-05-03 15:11:00',
 'CI_u67llhjql2eg',
 '2024-05-03 15:11:00',
 '2024-05-31 15:11:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 '8cti84h7@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_5r7z11',
 '01034556833',
 NULL, 'USER', 'ACTIVE',
 '2024-05-04 12:02:00',
 'CI_mse3tqr7p90v',
 '2024-05-04 12:02:00',
 '2024-05-31 12:02:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'eyu0l3ct@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_3mq3qs',
 '01034001196',
 NULL, 'USER', 'ACTIVE',
 '2024-05-05 18:37:00',
 'CI_zad6cklm4abf',
 '2024-05-05 18:37:00',
 '2024-09-21 18:37:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 '815so0j6@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_wxta0s',
 '01068772562',
 NULL, 'USER', 'ACTIVE',
 '2024-05-06 12:04:00',
 'CI_jrw10pqi2n8e',
 '2024-05-06 12:04:00',
 '2024-06-18 12:04:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'i65s6r6y@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_9g84gg',
 '01079987735',
 NULL, 'USER', 'ACTIVE',
 '2024-05-07 10:02:00',
 'CI_c0vi1bqcvgxu',
 '2024-05-07 10:02:00',
 '2024-07-14 10:02:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'socjuxvr@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_g7aiuh',
 '01059306367',
 NULL, 'USER', 'ACTIVE',
 '2024-05-08 11:08:00',
 'CI_n1bo5mwrr7sh',
 '2024-05-08 11:08:00',
 '2024-08-01 11:08:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'pph91x82@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_bavf1i',
 '01059066125',
 NULL, 'USER', 'ACTIVE',
 '2024-05-09 11:42:00',
 'CI_gajezwd4am0u',
 '2024-05-09 11:42:00',
 '2024-10-08 11:42:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'b71hardp@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_tlcoqz',
 '01072472135',
 NULL, 'USER', 'ACTIVE',
 '2024-05-10 17:07:00',
 'CI_rr6q8rgmj7tk',
 '2024-05-10 17:07:00',
 '2024-08-15 17:07:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 '4vqh1cg4@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_m4wr9e',
 '01085131567',
 NULL, 'USER', 'ACTIVE',
 '2024-05-11 15:28:00',
 'CI_cje32any4rr5',
 '2024-05-11 15:28:00',
 '2024-10-07 15:28:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 '69m0n67q@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_izvatk',
 '01013929929',
 NULL, 'USER', 'ACTIVE',
 '2024-05-12 10:49:00',
 'CI_77f1pxj07pic',
 '2024-05-12 10:49:00',
 '2024-06-21 10:49:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'zwven8fc@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_aqy13g',
 '01057448731',
 NULL, 'USER', 'ACTIVE',
 '2024-05-13 17:45:00',
 'CI_ovzw05fnpw7c',
 '2024-05-13 17:45:00',
 '2024-08-06 17:45:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 '85fftjz5@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_0asayn',
 '01073659431',
 NULL, 'USER', 'ACTIVE',
 '2024-05-14 09:49:00',
 'CI_6rjz755bfvkj',
 '2024-05-14 09:49:00',
 '2024-09-11 09:49:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 '5ohqac4a@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_2x67s8',
 '01051609715',
 NULL, 'USER', 'ACTIVE',
 '2024-05-15 09:29:00',
 'CI_riokbheeb73s',
 '2024-05-15 09:29:00',
 '2024-09-02 09:29:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 '2hqlmo92@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_fwfjbl',
 '01032967809',
 NULL, 'USER', 'ACTIVE',
 '2024-05-16 11:36:00',
 'CI_hu9qrexhu0r7',
 '2024-05-16 11:36:00',
 '2024-07-30 11:36:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'vwsfkas6@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_vsw2jq',
 '01024911957',
 NULL, 'USER', 'ACTIVE',
 '2024-05-17 12:39:00',
 'CI_6p6tnnv20pwc',
 '2024-05-17 12:39:00',
 '2024-06-12 12:39:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'gcs8c77u@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_ib8xxw',
 '01013139573',
 NULL, 'USER', 'ACTIVE',
 '2024-05-18 12:01:00',
 'CI_k87pw4whlwaj',
 '2024-05-18 12:01:00',
 '2024-10-01 12:01:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'kjfc6uyd@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_grbbx6',
 '01048476259',
 NULL, 'USER', 'ACTIVE',
 '2024-05-19 13:56:00',
 'CI_ci0u558t4j6j',
 '2024-05-19 13:56:00',
 '2024-07-14 13:56:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'xn47vz6f@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_mauojh',
 '01058702628',
 NULL, 'USER', 'ACTIVE',
 '2024-05-20 15:30:00',
 'CI_uplczfi7bbu6',
 '2024-05-20 15:30:00',
 '2024-11-11 15:30:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'ewr2n6n1@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_yhklq5',
 '01068948284',
 NULL, 'USER', 'ACTIVE',
 '2024-05-21 14:47:00',
 'CI_67r811221osr',
 '2024-05-21 14:47:00',
 '2024-05-29 14:47:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'awcvpw9b@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_ppgft4',
 '01076969312',
 NULL, 'USER', 'ACTIVE',
 '2024-05-22 13:16:00',
 'CI_plwwzyflprqg',
 '2024-05-22 13:16:00',
 '2024-10-17 13:16:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'lvnln7s7@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_oqflhw',
 '01010404091',
 NULL, 'USER', 'ACTIVE',
 '2024-05-23 14:22:00',
 'CI_ijpn9shw4z3i',
 '2024-05-23 14:22:00',
 '2024-08-04 14:22:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 '89uvqio5@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_0fiyel',
 '01020222421',
 NULL, 'USER', 'ACTIVE',
 '2024-05-24 16:31:00',
 'CI_odfm8xdrjgne',
 '2024-05-24 16:31:00',
 '2024-07-16 16:31:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 'lww5ueww@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_cyjnkx',
 '01067569699',
 NULL, 'USER', 'ACTIVE',
 '2024-05-25 10:34:00',
 'CI_1j26n0vw3boz',
 '2024-05-25 10:34:00',
 '2024-08-02 10:34:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'si2wtqkg@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_7qs7ee',
 '01052468503',
 NULL, 'USER', 'ACTIVE',
 '2024-05-26 12:36:00',
 'CI_6d7une5cfi79',
 '2024-05-26 12:36:00',
 '2024-12-09 12:36:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'xueuabr1@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_3ccwtq',
 '01067133287',
 NULL, 'USER', 'ACTIVE',
 '2024-05-27 14:09:00',
 'CI_zo06qnun1v4x',
 '2024-05-27 14:09:00',
 '2024-08-07 14:09:00',
 0, NULL, NULL, NULL, NULL,
 0, 'KAKAO', NULL, 0
),
(
 'b8tuhomf@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_rycns7',
 '01042453573',
 NULL, 'USER', 'ACTIVE',
 '2024-05-28 17:00:00',
 'CI_ouiryi05ntr6',
 '2024-05-28 17:00:00',
 '2024-06-09 17:00:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'jszlyy0u@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_e7zr8y',
 '01078936551',
 NULL, 'USER', 'ACTIVE',
 '2024-05-29 11:21:00',
 'CI_gtppysjm6s8l',
 '2024-05-29 11:21:00',
 '2024-12-11 11:21:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 '9zdiu56i@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_es1dk7',
 '01093334528',
 NULL, 'USER', 'ACTIVE',
 '2024-05-30 14:45:00',
 'CI_9waq7tpy28gl',
 '2024-05-30 14:45:00',
 '2024-06-27 14:45:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 '0rdds9n3@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_6cyddn',
 '01034699077',
 NULL, 'USER', 'ACTIVE',
 '2024-05-31 13:16:00',
 'CI_gv7dfcjoalpm',
 '2024-05-31 13:16:00',
 '2024-08-10 13:16:00',
 0, NULL, NULL, NULL, NULL,
 1, 'KAKAO', NULL, 0
),
(
 'yqhj1d8s@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_ysyjy7',
 '01018701800',
 NULL, 'USER', 'ACTIVE',
 '2024-06-01 10:01:00',
 'CI_8iirkmfa484h',
 '2024-06-01 10:01:00',
 '2024-10-25 10:01:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 '97rvva5v@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_e3ymwe',
 '01056283678',
 NULL, 'USER', 'ACTIVE',
 '2024-06-02 12:57:00',
 'CI_gmdaqql2qra1',
 '2024-06-02 12:57:00',
 '2024-12-13 12:57:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 '6ipqknoq@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_dh1i77',
 '01073087388',
 NULL, 'USER', 'ACTIVE',
 '2024-06-03 10:31:00',
 'CI_j1eeju0d50xj',
 '2024-06-03 10:31:00',
 '2024-11-19 10:31:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 '0f9fk10m@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_t34pym',
 '01023283605',
 NULL, 'USER', 'ACTIVE',
 '2024-06-04 14:16:00',
 'CI_mxnp50p48u3q',
 '2024-06-04 14:16:00',
 '2024-07-26 14:16:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 'x6hf1gbo@daum.net',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_t1ad9u',
 '01089147581',
 NULL, 'USER', 'ACTIVE',
 '2024-06-05 15:51:00',
 'CI_9layfob9lleu',
 '2024-06-05 15:51:00',
 '2024-06-20 15:51:00',
 0, NULL, NULL, NULL, NULL,
 1, 'GOOGLE', NULL, 0
),
(
 '7k71pbda@nate.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_1pf7h9',
 '01049997000',
 NULL, 'USER', 'ACTIVE',
 '2024-06-06 18:33:00',
 'CI_glpa2zv5m1sk',
 '2024-06-06 18:33:00',
 '2024-07-14 18:33:00',
 0, NULL, NULL, NULL, NULL,
 0, 'GOOGLE', NULL, 0
),
(
 'js1hbjbx@naver.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_h5nyy5',
 '01034429162',
 NULL, 'USER', 'ACTIVE',
 '2024-06-07 13:13:00',
 'CI_a1ezh5kowavz',
 '2024-06-07 13:13:00',
 '2024-11-11 13:13:00',
 0, NULL, NULL, NULL, NULL,
 0, 'LOCAL', NULL, 0
),
(
 'dxecvwtt@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_59y37r',
 '01031854040',
 NULL, 'USER', 'ACTIVE',
 '2024-06-08 10:28:00',
 'CI_le85o2bufhpk',
 '2024-06-08 10:28:00',
 '2024-06-23 10:28:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
),
(
 't1x1ihem@gmail.com',
 '$2a$10$Wln.vzRj7UQ0/ynhnfYv..BwQP2IOl.R9mNYsaPZLenBylXdBNmWq',
 '유저_3tiw8j',
 '01031725311',
 NULL, 'USER', 'ACTIVE',
 '2024-06-09 10:54:00',
 'CI_m6efhbvi58mt',
 '2024-06-09 10:54:00',
 '2024-09-13 10:54:00',
 0, NULL, NULL, NULL, NULL,
 1, 'LOCAL', NULL, 0
);


-- CATEGORY: 상품 카테고리
INSERT INTO CATEGORY (CATEGORY_ID, CATEGORY_NAME) VALUES
(1, 'AI'),
(2, 'MEDIA'),
(3, 'EDU'),
(4, 'MEMBER');

-- PRODUCT: 상품 정보 (쿠팡플레이 삭제, 이미지 경로 업데이트)
INSERT INTO `PRODUCT` (`PRODUCT_ID`, `CATEGORY_ID`, `PRODUCT_NAME`, `PRODUCT_STATUS`, `PRICE`, `IMAGE`, `MAX_SHARE`) VALUES
   (1, 1, 'Google AI Pro', 'ACTIVE', 17000, '/uploads/product-image/Google_AI_Logo.png', NULL),
   (2, 2, 'Disney+ Standard', 'ACTIVE', 9900, '/uploads/product-image/Disney_plus_logo.png', NULL),
   (3, 2, '왓챠 베이직', 'ACTIVE', 7900, '/uploads/product-image/WATCHA_Logo.png', NULL),
   (4, 2, '유튜브 프리미엄', 'ACTIVE', 13900, '/uploads/product-image/YouTube_logo.png', NULL),
   (5, 1, 'Chat GPT Plus', 'ACTIVE', 29000, '/uploads/product-image/ChatGPT_logo.png', NULL),
   -- (6번 쿠팡플레이 삭제)
   (7, 2, '티빙 스탠다드', 'ACTIVE', 10900, '/uploads/product-image/tving_logo.png', NULL),
   (8, 2, '웨이브 프리미엄', 'ACTIVE', 13900, '/uploads/product-image/wavve_logo.png', NULL),
   (9, 4, 'Naver 멤버십 1개월권', 'ACTIVE', 3000, '/uploads/product-image/naverplus_logo.png', NULL),
   (10, 4, 'Naver 멤버십 12개월권', 'ACTIVE', 30000, '/uploads/product-image/naverplus_logo.png', NULL),
   (11, 1, 'Chat GPT Pro', 'ACTIVE', 50000, '/uploads/product-image/chatgpt_logo.png', NULL),
   (12, 1, 'Google AI Ultra', 'ACTIVE', 330000, '/uploads/product-image/googleai_Logo.png', NULL),
   (13, 2, 'Disney+ Premium', 'ACTIVE', 13900, '/uploads/product-image/disneyplus_logo.png', NULL),
   -- (14번 쿠팡플레이 삭제)
   (15, 3, 'Skillshare Monthly', 'ACTIVE', 20600, '/uploads/product-image/Skillshare_logo.png', NULL),
   (16, 3, 'LinkedIn Learning Monthly', 'ACTIVE', 58900, '/uploads/product-image/LinkedIn_Learning_Logo.png', NULL),
   (17, 2, 'Disney+ + TVING Bundle', 'ACTIVE', 18000, '/uploads/product-image/Disney_plus_logo.png', NULL),
   (18, 2, 'Netflix Basic', 'ACTIVE', 9500, '/uploads/product-image/Netflix_logo.png', NULL),
   (19, 2, 'Netflix Standard', 'ACTIVE', 14500, '/uploads/product-image/Netflix_logo.png', NULL),
   (20, 2, 'Netflix Premium', 'ACTIVE', 19000, '/uploads/product-image/Netflix_logo.png', NULL),
   (21, 2, 'Netflix', 'ACTIVE', 19000, '/uploads/product-image/Netflix_logo.png', NULL);

   (15, 3, 'Skillshare Monthly', 'ACTIVE', 20600, '/uploads/product-image/skillshare_logo.png', NULL),
   (16, 3, 'LinkedIn Learning Monthly', 'ACTIVE', 58900, '/uploads/product-image/linkedinlearning_logo.png', NULL),
   (17, 2, 'Disney+ + TVING Bundle', 'ACTIVE', 18000, '/uploads/product-image/disneyplustving_logo.png', NULL),
   (18, 2, 'Netflix Basic', 'ACTIVE', 9500, '/uploads/product-image/netflix_logo.png', NULL),
   (19, 2, 'Netflix Standard', 'ACTIVE', 14500, '/uploads/product-image/netflix_logo.png', NULL),
   (20, 2, 'Netflix Premium', 'ACTIVE', 19000, '/uploads/product-image/netflix_logo.png', NULL),
   (21, 2, 'Netflix', 'ACTIVE', 19000, '/uploads/product-image/netflix_logo.png', NULL);

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
UPDATE CHATBOT_KNOWLEDGE SET EMBEDDING = @vec WHERE EMBEDDING IS NULL AND ID > 0;

-- OAUTH_ACCOUNT: 소셜 로그인 연동 (10명)
INSERT INTO OAUTH_ACCOUNT (
    OAUTH_ID,
    PROVIDER,
    PROVIDER_USER_ID,
    USER_ID,
    CONNECTED_DATE,
    RELEASE_DATE
)
SELECT
    CONCAT(
        LOWER(p.PROVIDER), '_',
        LPAD(@rn := @rn + 1, 3, '0'),
        '_',
        u.USER_ID
    ) AS OAUTH_ID,

    p.PROVIDER,

    CONCAT(
        LOWER(p.PROVIDER),
        '_uid_',
        LPAD(@rn2 := @rn2 + 1, 6, '0')
    ) AS PROVIDER_USER_ID,

    u.USER_ID,

    DATE_ADD(u.REG_DATE, INTERVAL 1 DAY) AS CONNECTED_DATE,

    NULL AS RELEASE_DATE
FROM USERS u
JOIN (
    SELECT 'KAKAO' AS PROVIDER
    UNION ALL
    SELECT 'GOOGLE'
) p
CROSS JOIN (SELECT @rn := 0, @rn2 := 0) vars
WHERE u.ROLE = 'USER'
LIMIT 10;


-- BLACKLIST: 블랙리스트 이력 (2명, 해제됨)
INSERT INTO BLACKLIST (
    USER_ID, REASON, STATUS, REG_DATE, RELEASE_DATE
)
SELECT
    u.USER_ID,

    CASE (u.rn % 5)
        WHEN 0 THEN '결제 실패 후 서비스 무단 이용 시도'
        WHEN 1 THEN '환불 악용 의심(반복 결제 후 취소)'
        WHEN 2 THEN '비정상 로그인 시도 다수(보안 위험)'
        WHEN 3 THEN '파티 규칙 미준수(지각/노쇼) 누적'
        ELSE '욕설/비방 등 커뮤니티 이용 규정 위반'
    END AS REASON,

    'ACTIVE' AS STATUS,

    DATE_ADD('2024-04-01', INTERVAL u.rn DAY) AS REG_DATE,

    NULL AS RELEASE_DATE
FROM (
    SELECT USER_ID, @rn1 := @rn1 + 1 AS rn
    FROM USERS, (SELECT @rn1 := 0) v
    WHERE ROLE = 'USER'
    ORDER BY USER_ID
    LIMIT 10
) u
WHERE NOT EXISTS (
    SELECT 1
    FROM BLACKLIST b
    WHERE b.USER_ID = u.USER_ID
      AND b.STATUS = 'ACTIVE'
);


INSERT INTO BLACKLIST (
    USER_ID, REASON, STATUS, REG_DATE, RELEASE_DATE
)
SELECT
    u.USER_ID,

    CASE (u.rn % 3)
        WHEN 0 THEN '파티 규칙 미준수 1차 경고'
        WHEN 1 THEN '계정 공유 의심 행위'
        ELSE '약관 위반 경고 누적'
    END AS REASON,

    'RELEASE' AS STATUS,

    DATE_ADD('2024-02-01', INTERVAL u.rn DAY) AS REG_DATE,

    DATE_ADD('2024-02-15', INTERVAL u.rn DAY) AS RELEASE_DATE
FROM (
    SELECT USER_ID, @rn2 := @rn2 + 1 AS rn
    FROM USERS, (SELECT @rn2 := 0) v
    WHERE ROLE = 'USER'
    ORDER BY USER_ID
    LIMIT 20
) u;

-- ============================================
-- 4. 계좌/카드/구독 데이터
-- ============================================

-- ACCOUNT: 정산 계좌 정보 (20명)
INSERT INTO ACCOUNT (
    USER_ID, BANK_CODE, BANK_NAME, ACCOUNT_NUMBER,
    ACCOUNT_HOLDER, IS_VERIFIED, REG_DATE, VERIFY_DATE
) VALUES
('shakkoum@naver.com', '088', '신한은행', 'ENC_ACC_0001', '유저_ww8nqg', 'Y', '2024-03-03 10:00:00', '2024-03-03 10:30:00'),
('jo3hz8jz@naver.com', '004', 'KB국민은행', 'ENC_ACC_0002', '유저_bdbris', 'Y', '2024-03-04 11:00:00', '2024-03-04 11:30:00'),
('vw51kceh@nate.com', '020', '우리은행', 'ENC_ACC_0003', '유저_o3t4tq', 'Y', '2024-03-05 12:00:00', '2024-03-05 12:30:00'),
('6etecsvu@daum.net', '081', '하나은행', 'ENC_ACC_0004', '유저_wu3yro', 'Y', '2024-03-06 13:00:00', '2024-03-06 13:30:00'),
('f92ii39x@gmail.com', '011', 'NH농협은행', 'ENC_ACC_0005', '유저_hpdkxu', 'Y', '2024-03-07 14:00:00', '2024-03-07 14:30:00'),

('681vf6nk@gmail.com', '088', '신한은행', 'ENC_ACC_0006', '유저_oiwjsk', 'Y', '2024-03-08 10:00:00', '2024-03-08 10:30:00'),
('7slvebq6@daum.net', '004', 'KB국민은행', 'ENC_ACC_0007', '유저_9ft98t', 'Y', '2024-03-09 11:00:00', '2024-03-09 11:30:00'),
('jlkc678n@gmail.com', '020', '우리은행', 'ENC_ACC_0008', '유저_qfjeaa', 'Y', '2024-03-10 12:00:00', '2024-03-10 12:30:00'),
('j7ixhpnb@gmail.com', '081', '하나은행', 'ENC_ACC_0009', '유저_zbdx73', 'Y', '2024-03-11 13:00:00', '2024-03-11 13:30:00'),
('169o4kxq@nate.com', '003', 'IBK기업은행', 'ENC_ACC_0010', '유저_q248os', 'Y', '2024-03-12 14:00:00', '2024-03-12 14:30:00'),

('dj6mbd5o@naver.com', '011', 'NH농협은행', 'ENC_ACC_0011', '유저_lhs17m', 'Y', '2024-03-13 10:00:00', '2024-03-13 10:30:00'),
('ytway0kx@naver.com', '088', '신한은행', 'ENC_ACC_0012', '유저_0f1du7', 'Y', '2024-03-14 11:00:00', '2024-03-14 11:30:00'),
('0spo51ch@naver.com', '004', 'KB국민은행', 'ENC_ACC_0013', '유저_g9n8o0', 'Y', '2024-03-15 12:00:00', '2024-03-15 12:30:00'),
('s06x8nic@daum.net', '020', '우리은행', 'ENC_ACC_0014', '유저_76taa4', 'Y', '2024-03-16 13:00:00', '2024-03-16 13:30:00'),
('6v1vjnub@naver.com', '081', '하나은행', 'ENC_ACC_0015', '유저_nelqnd', 'Y', '2024-03-17 14:00:00', '2024-03-17 14:30:00'),

('hb7thi3q@daum.net', '003', 'IBK기업은행', 'ENC_ACC_0016', '유저_on89lq', 'Y', '2024-03-18 10:00:00', '2024-03-18 10:30:00'),
('mj40lbjs@nate.com', '011', 'NH농협은행', 'ENC_ACC_0017', '유저_0sak6m', 'Y', '2024-03-19 11:00:00', '2024-03-19 11:30:00'),
('7u3uf22s@naver.com', '088', '신한은행', 'ENC_ACC_0018', '유저_c3zuwe', 'Y', '2024-03-20 12:00:00', '2024-03-20 12:30:00'),
('ymh0evgm@daum.net', '004', 'KB국민은행', 'ENC_ACC_0019', '유저_jeublh', 'Y', '2024-03-21 13:00:00', '2024-03-21 13:30:00'),
('y417brpe@daum.net', '020', '우리은행', 'ENC_ACC_0020', '유저_9f5zbz', 'Y', '2024-03-22 14:00:00', '2024-03-22 14:30:00');


-- USER_CARD: 사용자 카드 정보 (20명)
INSERT INTO USER_CARD (
    USER_ID,
    BILLING_KEY,
    CARD_COMPANY,
    CARD_NUMBER,
    REG_DATE
)
SELECT
    u.USER_ID,

    CONCAT(
        'BILLKEY_',
        LPAD(u.rn, 3, '0')
    ) AS BILLING_KEY,

    CASE (u.rn % 5)
        WHEN 0 THEN 'KB국민카드'
        WHEN 1 THEN '신한카드'
        WHEN 2 THEN '현대카드'
        WHEN 3 THEN '롯데카드'
        ELSE '삼성카드'
    END AS CARD_COMPANY,

    CASE (u.rn % 5)
        WHEN 0 THEN CONCAT('4012-****-****-', LPAD(u.rn, 4, '0'))
        WHEN 1 THEN CONCAT('5213-****-****-', LPAD(u.rn, 4, '0'))
        WHEN 2 THEN CONCAT('5412-****-****-', LPAD(u.rn, 4, '0'))
        WHEN 3 THEN CONCAT('5312-****-****-', LPAD(u.rn, 4, '0'))
        ELSE CONCAT('5512-****-****-', LPAD(u.rn, 4, '0'))
    END AS CARD_NUMBER,

    DATE_ADD(u.REG_DATE, INTERVAL 1 DAY) AS REG_DATE

FROM (
    SELECT
        USER_ID,
        REG_DATE,
        @rn := @rn + 1 AS rn
    FROM USERS, (SELECT @rn := 0) r
    WHERE ROLE = 'USER'
    ORDER BY USER_ID
    LIMIT 20
) u
WHERE NOT EXISTS (
    SELECT 1
    FROM USER_CARD c
    WHERE c.USER_ID = u.USER_ID
);


-- SUBSCRIPTION: 구독 정보 (20명)
INSERT INTO SUBSCRIPTION (
    USER_ID,
    PRODUCT_ID,
    SUBSCRIPTION_STATUS,
    START_DATE,
    END_DATE,
    CANCEL_REASON,
    CANCEL_DATE
)
SELECT
    u.USER_ID,
    p.PRODUCT_ID,
    'ACTIVE',
    DATE_ADD('2024-04-01', INTERVAL u.rn * 3 DAY),
    NULL,
    NULL,
    NULL
FROM (
    SELECT
        USER_ID,
        @rn := @rn + 1 AS rn
    FROM USERS, (SELECT @rn := 0) r
    WHERE ROLE = 'USER'
    ORDER BY USER_ID
    LIMIT 20
) u
JOIN (
    SELECT PRODUCT_ID
    FROM PRODUCT
    ORDER BY PRODUCT_ID
) p
  ON p.PRODUCT_ID = (
      SELECT PRODUCT_ID
      FROM PRODUCT
      ORDER BY PRODUCT_ID
      LIMIT 1 OFFSET (u.rn % (SELECT COUNT(*) FROM PRODUCT))
  )
WHERE NOT EXISTS (
    SELECT 1
    FROM SUBSCRIPTION s
    WHERE s.USER_ID = u.USER_ID
      AND s.SUBSCRIPTION_STATUS = 'ACTIVE'
);



INSERT INTO PAYMENT_RETRY_HISTORY (
    PAYMENT_ID,
    PARTY_ID,
    PARTY_MEMBER_ID,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_REASON,
    RETRY_STATUS,
    NEXT_RETRY_DATE,
    ERROR_CODE,
    ERROR_MESSAGE
)
SELECT
    p.PAYMENT_ID,
    p.PARTY_ID,
    p.PARTY_MEMBER_ID,
    1,
    DATE_SUB(NOW(), INTERVAL (24 + p.PAYMENT_ID % 24) HOUR),
    NULL,
    CASE WHEN p.PAYMENT_ID % 3 = 0 THEN 'FAILED' ELSE 'SUCCESS' END,
    CASE
        WHEN p.PAYMENT_ID % 3 = 0
        THEN DATE_SUB(NOW(), INTERVAL (12 + p.PAYMENT_ID % 12) HOUR)
        ELSE NULL
    END,
    CASE WHEN p.PAYMENT_ID % 3 = 0 THEN 'INSUFFICIENT_FUNDS' ELSE NULL END,
    CASE WHEN p.PAYMENT_ID % 3 = 0 THEN '잔액이 부족합니다.' ELSE NULL END
FROM PAYMENT p
WHERE p.PARTY_ID IS NOT NULL
  AND p.PARTY_MEMBER_ID IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM PAYMENT_RETRY_HISTORY h
      WHERE h.PAYMENT_ID = p.PAYMENT_ID
        AND h.ATTEMPT_NUMBER = 1
  );





INSERT INTO PAYMENT_RETRY_HISTORY (
    PAYMENT_ID,
    PARTY_ID,
    PARTY_MEMBER_ID,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_REASON,
    RETRY_STATUS,
    NEXT_RETRY_DATE,
    ERROR_CODE,
    ERROR_MESSAGE
)
SELECT
    h.PAYMENT_ID,
    h.PARTY_ID,
    h.PARTY_MEMBER_ID,
    2,
    h.NEXT_RETRY_DATE,
    h.ERROR_MESSAGE,
    CASE WHEN h.PAYMENT_ID % 2 = 0 THEN 'SUCCESS' ELSE 'FAILED' END,
    CASE WHEN h.PAYMENT_ID % 2 = 1 THEN DATE_SUB(NOW(), INTERVAL 1 HOUR) ELSE NULL END,
    CASE WHEN h.PAYMENT_ID % 2 = 1 THEN 'EXCEED_MAX_CARD_LIMIT' ELSE NULL END,
    CASE WHEN h.PAYMENT_ID % 2 = 1 THEN '카드 한도를 초과했습니다.' ELSE NULL END
FROM PAYMENT_RETRY_HISTORY h
WHERE h.ATTEMPT_NUMBER = 1
  AND h.RETRY_STATUS = 'FAILED'
  AND h.NEXT_RETRY_DATE IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM PAYMENT_RETRY_HISTORY x
      WHERE x.PAYMENT_ID = h.PAYMENT_ID
        AND x.ATTEMPT_NUMBER = 2
  );
  
  
  INSERT INTO PAYMENT_RETRY_HISTORY (
    PAYMENT_ID,
    PARTY_ID,
    PARTY_MEMBER_ID,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_REASON,
    RETRY_STATUS,
    NEXT_RETRY_DATE,
    ERROR_CODE,
    ERROR_MESSAGE
)
SELECT
    h.PAYMENT_ID,
    h.PARTY_ID,
    h.PARTY_MEMBER_ID,
    3,
    h.NEXT_RETRY_DATE,
    h.ERROR_MESSAGE,
    CASE WHEN h.PAYMENT_ID % 4 = 0 THEN 'SUCCESS' ELSE 'FAILED' END,
    NULL,
    CASE WHEN h.PAYMENT_ID % 4 <> 0 THEN 'CARD_SUSPENDED' ELSE NULL END,
    CASE WHEN h.PAYMENT_ID % 4 <> 0 THEN '카드가 정지되었습니다.' ELSE NULL END
FROM PAYMENT_RETRY_HISTORY h
WHERE h.ATTEMPT_NUMBER = 2
  AND h.RETRY_STATUS = 'FAILED'
  AND h.NEXT_RETRY_DATE IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM PAYMENT_RETRY_HISTORY x
      WHERE x.PAYMENT_ID = h.PAYMENT_ID
        AND x.ATTEMPT_NUMBER = 3
  );



-- ============================================
-- 7. 정산 데이터
-- ============================================

-- SETTLEMENT: 정산 5건
INSERT INTO SETTLEMENT (
    PARTY_ID,
    PARTY_LEADER_ID,
    ACCOUNT_ID,
    SETTLEMENT_MONTH,
    TOTAL_AMOUNT,
    COMMISSION_AMOUNT,
    NET_AMOUNT,
    SETTLEMENT_STATUS,
    SETTLEMENT_DATE,
    BANK_TRAN_ID
)
SELECT
    p.PARTY_ID,
    p.PARTY_LEADER_ID,
    p.ACCOUNT_ID,

    DATE_FORMAT(p.START_DATE, '%Y-%m') AS SETTLEMENT_MONTH,

    p.MONTHLY_FEE AS TOTAL_AMOUNT,

    FLOOR(p.MONTHLY_FEE * 0.15) AS COMMISSION_AMOUNT,

    p.MONTHLY_FEE - FLOOR(p.MONTHLY_FEE * 0.15) AS NET_AMOUNT,

    'COMPLETED' AS SETTLEMENT_STATUS,

    DATE_ADD(p.START_DATE, INTERVAL 1 MONTH) AS SETTLEMENT_DATE,

    CONCAT(
        'T',
        DATE_FORMAT(DATE_ADD(p.START_DATE, INTERVAL 1 MONTH), '%Y%m%d'),
        LPAD(p.PARTY_ID, 4, '0')
    ) AS BANK_TRAN_ID
FROM PARTY p
WHERE p.PARTY_STATUS = 'ACTIVE'
  AND p.ACCOUNT_ID IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM SETTLEMENT s
      WHERE s.PARTY_ID = p.PARTY_ID
  )
LIMIT 5;


-- REFUND_RETRY_HISTORY: 보증금 환불 재시도 이력
INSERT INTO REFUND_RETRY_HISTORY (
    DEPOSIT_ID,
    TOSS_PAYMENT_KEY,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_STATUS,
    RETRY_TYPE,
    NEXT_RETRY_DATE,
    REFUND_AMOUNT,
    REFUND_REASON,
    ERROR_CODE,
    ERROR_MESSAGE
)
SELECT
    d.DEPOSIT_ID,
    d.TOSS_PAYMENT_KEY,
    1,
    DATE_SUB(NOW(), INTERVAL (12 + d.DEPOSIT_ID % 24) HOUR),
    CASE WHEN d.DEPOSIT_ID % 3 = 0 THEN 'FAILED' ELSE 'SUCCESS' END,
    CASE WHEN d.DEPOSIT_TYPE = 'LEADER' THEN 'COMPENSATION' ELSE 'REFUND' END,
    CASE
        WHEN d.DEPOSIT_ID % 3 = 0
        THEN DATE_SUB(NOW(), INTERVAL (2 + d.DEPOSIT_ID % 6) HOUR)
        ELSE NULL
    END,
    COALESCE(d.REFUND_AMOUNT, d.DEPOSIT_AMOUNT),
    '환불 처리',
    CASE WHEN d.DEPOSIT_ID % 3 = 0 THEN 'TEMPORARY_ERROR' ELSE NULL END,
    CASE WHEN d.DEPOSIT_ID % 3 = 0 THEN '일시적인 오류가 발생했습니다.' ELSE NULL END
FROM DEPOSIT d
WHERE d.DEPOSIT_STATUS IN ('REFUNDED', 'PARTIAL_REFUNDED')
AND NOT EXISTS (
    SELECT 1
    FROM REFUND_RETRY_HISTORY h
    WHERE h.DEPOSIT_ID = d.DEPOSIT_ID
      AND h.ATTEMPT_NUMBER = 1
);



INSERT INTO REFUND_RETRY_HISTORY (
    DEPOSIT_ID,
    TOSS_PAYMENT_KEY,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_STATUS,
    RETRY_TYPE,
    NEXT_RETRY_DATE,
    REFUND_AMOUNT,
    REFUND_REASON,
    ERROR_CODE,
    ERROR_MESSAGE
)
SELECT
    h.DEPOSIT_ID,
    h.TOSS_PAYMENT_KEY,
    2,
    h.NEXT_RETRY_DATE,
    CASE WHEN h.DEPOSIT_ID % 2 = 0 THEN 'SUCCESS' ELSE 'FAILED' END,
    h.RETRY_TYPE,
    CASE WHEN h.DEPOSIT_ID % 2 = 1 THEN DATE_SUB(NOW(), INTERVAL 2 HOUR) ELSE NULL END,
    h.REFUND_AMOUNT,
    h.REFUND_REASON,
    CASE WHEN h.DEPOSIT_ID % 2 = 1 THEN 'INVALID_PAYMENT_KEY' ELSE NULL END,
    CASE WHEN h.DEPOSIT_ID % 2 = 1 THEN '유효하지 않은 결제 키입니다.' ELSE NULL END
FROM REFUND_RETRY_HISTORY h
WHERE h.ATTEMPT_NUMBER = 1
  AND h.RETRY_STATUS = 'FAILED'
  AND h.NEXT_RETRY_DATE IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM REFUND_RETRY_HISTORY x
      WHERE x.DEPOSIT_ID = h.DEPOSIT_ID
        AND x.ATTEMPT_NUMBER = 2
  );



INSERT INTO REFUND_RETRY_HISTORY (
    DEPOSIT_ID,
    TOSS_PAYMENT_KEY,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_STATUS,
    RETRY_TYPE,
    NEXT_RETRY_DATE,
    REFUND_AMOUNT,
    REFUND_REASON,
    ERROR_CODE,
    ERROR_MESSAGE
)
SELECT
    h.DEPOSIT_ID,
    h.TOSS_PAYMENT_KEY,
    3,
    h.NEXT_RETRY_DATE,
    CASE WHEN h.DEPOSIT_ID % 4 = 0 THEN 'SUCCESS' ELSE 'FAILED' END,
    h.RETRY_TYPE,
    NULL,
    h.REFUND_AMOUNT,
    h.REFUND_REASON,
    CASE WHEN h.DEPOSIT_ID % 4 <> 0 THEN 'PAYMENT_NOT_FOUND' ELSE NULL END,
    CASE WHEN h.DEPOSIT_ID % 4 <> 0 THEN '결제 정보를 찾을 수 없습니다.' ELSE NULL END
FROM REFUND_RETRY_HISTORY h
WHERE h.ATTEMPT_NUMBER = 2
  AND h.RETRY_STATUS = 'FAILED'
  AND h.NEXT_RETRY_DATE IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM REFUND_RETRY_HISTORY x
      WHERE x.DEPOSIT_ID = h.DEPOSIT_ID
        AND x.ATTEMPT_NUMBER = 3
  );


-- SETTLEMENT_RETRY_HISTORY: 정산 이체 재시도 이력
INSERT INTO SETTLEMENT_RETRY_HISTORY (
    SETTLEMENT_ID,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_REASON,
    RETRY_STATUS,
    NEXT_RETRY_DATE,
    TRANSFER_AMOUNT,
    ERROR_CODE,
    ERROR_MESSAGE,
    BANK_RSP_CODE,
    BANK_RSP_MESSAGE,
    BANK_TRAN_ID
)
SELECT
    s.SETTLEMENT_ID,
    1,
    DATE_SUB(s.SETTLEMENT_DATE, INTERVAL 5 MINUTE),
    NULL,
    CASE WHEN s.SETTLEMENT_ID % 3 = 0 THEN 'FAILED' ELSE 'SUCCESS' END,
    CASE
        WHEN s.SETTLEMENT_ID % 3 = 0
        THEN DATE_ADD(s.SETTLEMENT_DATE, INTERVAL 2 HOUR)
        ELSE NULL
    END,
    s.NET_AMOUNT,
    CASE WHEN s.SETTLEMENT_ID % 3 = 0 THEN 'A0003' ELSE NULL END,
    CASE WHEN s.SETTLEMENT_ID % 3 = 0 THEN '수취인 계좌 오류' ELSE NULL END,
    CASE WHEN s.SETTLEMENT_ID % 3 = 0 THEN '301' ELSE '000' END,
    CASE WHEN s.SETTLEMENT_ID % 3 = 0 THEN '수취계좌오류' ELSE '정상처리' END,
    CASE
        WHEN s.SETTLEMENT_ID % 3 <> 0
        THEN CONCAT('T', DATE_FORMAT(s.SETTLEMENT_DATE, '%Y%m%d'), LPAD(s.SETTLEMENT_ID, 4, '0'))
        ELSE NULL
    END
FROM SETTLEMENT s
WHERE NOT EXISTS (
    SELECT 1
    FROM SETTLEMENT_RETRY_HISTORY h
    WHERE h.SETTLEMENT_ID = s.SETTLEMENT_ID
      AND h.ATTEMPT_NUMBER = 1
);

INSERT INTO SETTLEMENT_RETRY_HISTORY (
    SETTLEMENT_ID,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_REASON,
    RETRY_STATUS,
    NEXT_RETRY_DATE,
    TRANSFER_AMOUNT,
    ERROR_CODE,
    ERROR_MESSAGE,
    BANK_RSP_CODE,
    BANK_RSP_MESSAGE,
    BANK_TRAN_ID
)
SELECT
    h.SETTLEMENT_ID,
    2 AS ATTEMPT_NUMBER,
    h.NEXT_RETRY_DATE AS ATTEMPT_DATE,
    h.ERROR_MESSAGE AS RETRY_REASON,
    CASE
        WHEN h.SETTLEMENT_ID % 2 = 0 THEN 'SUCCESS'
        ELSE 'FAILED'
    END AS RETRY_STATUS,
    CASE
        WHEN h.SETTLEMENT_ID % 2 = 1
        THEN DATE_ADD(h.NEXT_RETRY_DATE, INTERVAL 2 HOUR)
        ELSE NULL
    END AS NEXT_RETRY_DATE,
    h.TRANSFER_AMOUNT,
    CASE
        WHEN h.SETTLEMENT_ID % 2 = 1 THEN 'A0005'
        ELSE NULL
    END AS ERROR_CODE,
    CASE
        WHEN h.SETTLEMENT_ID % 2 = 1 THEN '출금 한도 초과'
        ELSE NULL
    END AS ERROR_MESSAGE,
    CASE
        WHEN h.SETTLEMENT_ID % 2 = 1 THEN '512'
        ELSE '000'
    END AS BANK_RSP_CODE,
    CASE
        WHEN h.SETTLEMENT_ID % 2 = 1 THEN '출금한도초과'
        ELSE '정상처리'
    END AS BANK_RSP_MESSAGE,
    CASE
        WHEN h.SETTLEMENT_ID % 2 = 0
        THEN CONCAT(
            'T',
            DATE_FORMAT(h.ATTEMPT_DATE, '%Y%m%d'),
            LPAD(h.SETTLEMENT_ID, 4, '0')
        )
        ELSE NULL
    END AS BANK_TRAN_ID
FROM SETTLEMENT_RETRY_HISTORY h
WHERE h.ATTEMPT_NUMBER = 1
  AND h.RETRY_STATUS = 'FAILED';
  

INSERT INTO SETTLEMENT_RETRY_HISTORY (
    SETTLEMENT_ID,
    ATTEMPT_NUMBER,
    ATTEMPT_DATE,
    RETRY_REASON,
    RETRY_STATUS,
    NEXT_RETRY_DATE,
    TRANSFER_AMOUNT,
    ERROR_CODE,
    ERROR_MESSAGE,
    BANK_RSP_CODE,
    BANK_RSP_MESSAGE,
    BANK_TRAN_ID
)
SELECT
    h.SETTLEMENT_ID,
    3 AS ATTEMPT_NUMBER,
    h.NEXT_RETRY_DATE AS ATTEMPT_DATE,
    h.ERROR_MESSAGE AS RETRY_REASON,
    CASE
        WHEN h.SETTLEMENT_ID % 4 = 0 THEN 'SUCCESS'
        ELSE 'FAILED'
    END AS RETRY_STATUS,
    NULL AS NEXT_RETRY_DATE,
    h.TRANSFER_AMOUNT,
    CASE
        WHEN h.SETTLEMENT_ID % 4 <> 0 THEN 'A0001'
        ELSE NULL
    END AS ERROR_CODE,
    CASE
        WHEN h.SETTLEMENT_ID % 4 <> 0 THEN '계좌번호 없음 - 수동 처리 필요'
        ELSE NULL
    END AS ERROR_MESSAGE,
    CASE
        WHEN h.SETTLEMENT_ID % 4 <> 0 THEN '115'
        ELSE '000'
    END AS BANK_RSP_CODE,
    CASE
        WHEN h.SETTLEMENT_ID % 4 <> 0 THEN '해당계좌없음'
        ELSE '정상처리'
    END AS BANK_RSP_MESSAGE,
    CASE
        WHEN h.SETTLEMENT_ID % 4 = 0
        THEN CONCAT(
            'T',
            DATE_FORMAT(h.ATTEMPT_DATE, '%Y%m%d'),
            LPAD(h.SETTLEMENT_ID, 4, '0')
        )
        ELSE NULL
    END AS BANK_TRAN_ID
FROM SETTLEMENT_RETRY_HISTORY h
WHERE h.ATTEMPT_NUMBER = 2
  AND h.RETRY_STATUS = 'FAILED';


-- ============================================
-- 8. 게시판 데이터
-- ============================================

-- COMMUNITY: 공지사항 3건 + 문의 7건
INSERT INTO COMMUNITY (
    USER_ID,
    COMMUNITY_CODE_ID,
    TITLE,
    CONTENT,
    CREATED_AT,
    VIEW_COUNT,
    FILE_ORIGINAL,
    FILE_UUID,
    ANSWER_CONTENT,
    ANSWERED_AT,
    ANSWER_STATUS
)
SELECT
    u.USER_ID,
    c.COMMUNITY_CODE_ID,
    c.TITLE,
    c.CONTENT,
    c.CREATED_AT,
    c.VIEW_COUNT,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
FROM (
    SELECT USER_ID
    FROM USERS
    WHERE ROLE = 'ADMIN'
    LIMIT 1
) u
JOIN (
    SELECT 10 AS COMMUNITY_CODE_ID,
           '[공지-시스템] MOA 서비스 정식 오픈 안내' AS TITLE,
           'MOA OTT 구독 공유 서비스가 정식 오픈하였습니다.' AS CONTENT,
           '2024-04-01 09:00:00' AS CREATED_AT,
           1523 AS VIEW_COUNT
    UNION ALL
    SELECT 4,
           '[FAQ] 파티 가입 방법이 궁금해요',
           '원하는 OTT 서비스를 선택한 후 모집 중인 파티에 가입하세요.',
           '2024-04-01 10:00:00',
           3421
    UNION ALL
    SELECT 4,
           '[FAQ] 보증금은 언제 환불되나요?',
           '파티 정상 종료 시 보증금 전액이 환불됩니다.',
           '2024-04-01 10:30:00',
           2876
) c;




INSERT INTO COMMUNITY (
    USER_ID,
    COMMUNITY_CODE_ID,
    TITLE,
    CONTENT,
    CREATED_AT,
    VIEW_COUNT,
    FILE_ORIGINAL,
    FILE_UUID,
    ANSWER_CONTENT,
    ANSWERED_AT,
    ANSWER_STATUS
)
SELECT
    u.USER_ID,
    q.COMMUNITY_CODE_ID,
    q.TITLE,
    q.CONTENT,
    q.CREATED_AT,
    NULL AS VIEW_COUNT,
    q.FILE_ORIGINAL,
    q.FILE_UUID,
    q.ANSWER_CONTENT,
    q.ANSWERED_AT,
    '답변완료' AS ANSWER_STATUS
FROM (
    SELECT USER_ID
    FROM USERS
    WHERE ROLE = 'USER'
    ORDER BY USER_ID
    LIMIT 7
) u
JOIN (
    SELECT 1 AS COMMUNITY_CODE_ID,
           '휴대폰 번호 변경 후 본인인증이 안돼요' AS TITLE,
           '최근 휴대폰 번호를 변경했는데 본인인증이 계속 실패합니다.' AS CONTENT,
           '2024-11-25 09:30:00' AS CREATED_AT,
           NULL AS FILE_ORIGINAL,
           NULL AS FILE_UUID,
           '번호 변경 시 통신사 정보 업데이트에 최대 24시간이 소요될 수 있습니다.' AS ANSWER_CONTENT,
           '2024-11-25 14:00:00' AS ANSWERED_AT
    UNION ALL
    SELECT 1,
           '프로필 이미지 업로드가 안됩니다',
           '프로필 이미지를 변경하려고 하는데 계속 오류가 발생합니다.',
           '2024-11-26 10:15:00',
           'profile_error.png',
           'uuid_profile_001.png',
           '이미지 형식을 JPG 또는 PNG로 변경 후 다시 시도해주세요.',
           '2024-11-26 15:30:00'
    UNION ALL
    SELECT 2,
           '이번 달 결제가 두 번 됐어요',
           '11월 1일에 결제가 끝났는데 11월 3일에 또 결제가 되었습니다.',
           '2024-11-05 08:30:00',
           'payment_double.png',
           'uuid_payment_001.png',
           '확인 결과 11월 1일 결제 실패 후 3일에 재결제가 진행되었습니다.',
           '2024-11-05 11:00:00'
    UNION ALL
    SELECT 3,
           '파티장이 계정 정보를 안 알려줘요',
           '파티에 가입했는데 파티장이 OTT 계정 정보를 공유하지 않습니다.',
           '2024-11-08 09:00:00',
           NULL,
           NULL,
           '파티장에게 알림을 발송하였습니다.',
           '2024-11-08 10:30:00'
    UNION ALL
    SELECT 3,
           '파티원이 비밀번호를 변경했어요',
           '파티원 중 한 명이 OTT 비밀번호를 임의로 변경해서 접속이 안 됩니다.',
           '2024-11-12 14:00:00',
           NULL,
           NULL,
           '해당 파티원에게 경고 조치하였습니다.',
           '2024-11-12 16:00:00'
    UNION ALL
    SELECT 3,
           '이번 달 정산금이 입금되지 않았어요',
           '매월 5일에 정산금이 들어오는데 오늘 10일인데도 입금이 안 됐습니다.',
           '2024-11-10 09:00:00',
           NULL,
           NULL,
           '확인 결과 계좌번호 오류로 입금이 반려되었습니다.',
           '2024-11-10 11:00:00'
    UNION ALL
    SELECT 3,
           '앱이 자꾸 튕겨요',
           '앱을 실행하면 메인 화면에서 계속 튕깁니다.',
           '2024-11-15 09:00:00',
           NULL,
           NULL,
           '앱 삭제 후 재설치를 시도해주세요.',
           '2024-11-15 11:30:00'
) q
ON TRUE;

-- ============================================
-- 9. 푸시 알림 데이터
-- ============================================

-- PUSH: 푸시 알림 10건
INSERT INTO PUSH (
    RECEIVER_ID,
    PUSH_CODE,
    TITLE,
    CONTENT,
    MODULE_ID,
    MODULE_TYPE,
    SENT_AT,
    READ_AT,
    IS_READ,
    IS_DELETED
)
SELECT
    p.USER_ID AS RECEIVER_ID,
    'PAY_SUCCESS' AS PUSH_CODE,
    '결제 완료' AS TITLE,
    CONCAT(
        DATE_FORMAT(p.PAYMENT_DATE, '%Y-%m'),
        ' 구독료 ',
        FORMAT(p.AMOUNT, 0),
        '원이 결제되었습니다.'
    ) AS CONTENT,
    p.PAYMENT_ID AS MODULE_ID,
    'PAYMENT' AS MODULE_TYPE,
    p.PAYMENT_DATE AS SENT_AT,
    CASE
        WHEN p.PAYMENT_ID % 2 = 0
        THEN DATE_ADD(p.PAYMENT_DATE, INTERVAL 8 HOUR)
        ELSE NULL
    END AS READ_AT,
    CASE
        WHEN p.PAYMENT_ID % 2 = 0 THEN 'Y'
        ELSE 'N'
    END AS IS_READ,
    'N' AS IS_DELETED
FROM PAYMENT p;





INSERT INTO PUSH (
    RECEIVER_ID,
    PUSH_CODE,
    TITLE,
    CONTENT,
    MODULE_ID,
    MODULE_TYPE,
    SENT_AT,
    READ_AT,
    IS_READ,
    IS_DELETED
)
SELECT
    pm.USER_ID AS RECEIVER_ID,
    'PARTY_START' AS PUSH_CODE,
    '파티 시작' AS TITLE,
    '참여 중인 파티가 시작되었습니다. OTT 계정 정보를 확인하세요.' AS CONTENT,
    p.PARTY_ID AS MODULE_ID,
    'PARTY' AS MODULE_TYPE,
    p.START_DATE AS SENT_AT,
    CASE
        WHEN pm.PARTY_MEMBER_ID % 2 = 0
        THEN DATE_ADD(p.START_DATE, INTERVAL 7 HOUR)
        ELSE NULL
    END AS READ_AT,
    CASE
        WHEN pm.PARTY_MEMBER_ID % 2 = 0 THEN 'Y'
        ELSE 'N'
    END AS IS_READ,
    'N' AS IS_DELETED
FROM PARTY p
JOIN PARTY_MEMBER pm
  ON pm.PARTY_ID = p.PARTY_ID
WHERE p.PARTY_STATUS = 'ACTIVE';



INSERT INTO PUSH (
    RECEIVER_ID,
    PUSH_CODE,
    TITLE,
    CONTENT,
    MODULE_ID,
    MODULE_TYPE,
    SENT_AT,
    READ_AT,
    IS_READ,
    IS_DELETED
)
SELECT
    pm.USER_ID AS RECEIVER_ID,
    'PARTY_JOIN' AS PUSH_CODE,
    '파티 가입 완료' AS TITLE,
    '파티 가입이 성공적으로 완료되었습니다.' AS CONTENT,
    pm.PARTY_ID AS MODULE_ID,
    'PARTY' AS MODULE_TYPE,
    pm.JOIN_DATE AS SENT_AT,
    DATE_ADD(pm.JOIN_DATE, INTERVAL 30 MINUTE) AS READ_AT,
    'Y' AS IS_READ,
    'N' AS IS_DELETED
FROM PARTY_MEMBER pm
WHERE pm.MEMBER_ROLE = 'MEMBER';



INSERT INTO PUSH (
    RECEIVER_ID,
    PUSH_CODE,
    TITLE,
    CONTENT,
    MODULE_ID,
    MODULE_TYPE,
    SENT_AT,
    READ_AT,
    IS_READ,
    IS_DELETED
)
SELECT
    s.PARTY_LEADER_ID AS RECEIVER_ID,
    'SETTLE_COMPLETED' AS PUSH_CODE,
    '정산 입금 완료' AS TITLE,
    CONCAT(
        s.SETTLEMENT_MONTH,
        ' 정산금 ',
        FORMAT(s.NET_AMOUNT, 0),
        '원이 입금되었습니다.'
    ) AS CONTENT,
    s.SETTLEMENT_ID AS MODULE_ID,
    'SETTLEMENT' AS MODULE_TYPE,
    s.SETTLEMENT_DATE AS SENT_AT,
    DATE_ADD(s.SETTLEMENT_DATE, INTERVAL 1 HOUR) AS READ_AT,
    'Y' AS IS_READ,
    'N' AS IS_DELETED
FROM SETTLEMENT s
WHERE s.SETTLEMENT_STATUS = 'COMPLETED';



INSERT INTO PUSH (
    RECEIVER_ID,
    PUSH_CODE,
    TITLE,
    CONTENT,
    MODULE_ID,
    MODULE_TYPE,
    SENT_AT,
    READ_AT,
    IS_READ,
    IS_DELETED
)
SELECT
    c.USER_ID AS RECEIVER_ID,
    'INQUIRY_ANSWER' AS PUSH_CODE,
    '문의 답변 완료' AS TITLE,
    '등록하신 문의에 답변이 완료되었습니다.' AS CONTENT,
    c.COMMUNITY_ID AS MODULE_ID,
    'COMMUNITY' AS MODULE_TYPE,
    c.ANSWERED_AT AS SENT_AT,
    DATE_ADD(c.ANSWERED_AT, INTERVAL 1 HOUR) AS READ_AT,
    'Y' AS IS_READ,
    'N' AS IS_DELETED
FROM COMMUNITY c
WHERE c.ANSWER_STATUS = '답변완료';


-- ============================================
-- 10. 계좌 인증 및 이체 거래 데이터
-- ============================================

-- ACCOUNT_VERIFICATION: 1원 인증 세션 (다양한 상태)
INSERT INTO ACCOUNT_VERIFICATION (
    USER_ID,
    BANK_TRAN_ID,
    BANK_CODE,
    ACCOUNT_NUM,
    ACCOUNT_HOLDER,
    VERIFY_CODE,
    ATTEMPT_COUNT,
    STATUS,
    EXPIRED_AT,
    CREATED_AT
)
SELECT
    a.USER_ID,

    CONCAT(
        'T',
        DATE_FORMAT(NOW(), '%Y%m%d'),
        LPAD(a.ACCOUNT_ID, 4, '0')
    ) AS BANK_TRAN_ID,

    a.BANK_CODE,
    a.ACCOUNT_NUMBER AS ACCOUNT_NUM,
    a.ACCOUNT_HOLDER,

    LPAD((a.ACCOUNT_ID * 137) % 10000, 4, '0') AS VERIFY_CODE,

    CASE
        WHEN a.ACCOUNT_ID % 4 = 0 THEN 0
        WHEN a.ACCOUNT_ID % 4 = 1 THEN 1
        WHEN a.ACCOUNT_ID % 4 = 2 THEN 3
        ELSE 0
    END AS ATTEMPT_COUNT,

    CASE
        WHEN a.ACCOUNT_ID % 4 = 0 THEN 'PENDING'
        WHEN a.ACCOUNT_ID % 4 = 1 THEN 'VERIFIED'
        WHEN a.ACCOUNT_ID % 4 = 2 THEN 'FAILED'
        ELSE 'EXPIRED'
    END AS STATUS,

    CASE
        WHEN a.ACCOUNT_ID % 4 = 3
        THEN DATE_SUB(NOW(), INTERVAL 5 MINUTE)
        ELSE DATE_ADD(NOW(), INTERVAL 5 MINUTE)
    END AS EXPIRED_AT,

    CASE
        WHEN a.ACCOUNT_ID % 4 = 1
        THEN DATE_SUB(NOW(), INTERVAL 10 MINUTE)
        WHEN a.ACCOUNT_ID % 4 = 2
        THEN DATE_SUB(NOW(), INTERVAL 2 MINUTE)
        ELSE NOW()
    END AS CREATED_AT
FROM ACCOUNT a
JOIN USERS u
  ON u.USER_ID = a.USER_ID
WHERE NOT EXISTS (
    SELECT 1
    FROM ACCOUNT_VERIFICATION v
    WHERE v.USER_ID = a.USER_ID
);

INSERT INTO DEPOSIT (
    PARTY_ID,
    PARTY_MEMBER_ID,
    USER_ID,
    DEPOSIT_TYPE,
    DEPOSIT_AMOUNT,
    DEPOSIT_STATUS,
    PAYMENT_DATE,
    REFUND_DATE,
    REFUND_AMOUNT,
    TOSS_PAYMENT_KEY,
    ORDER_ID
)
SELECT
    pm.PARTY_ID,
    pm.PARTY_MEMBER_ID,
    pm.USER_ID,

    CASE
        WHEN pm.MEMBER_ROLE = 'LEADER' THEN 'LEADER'
        ELSE 'SECURITY'
    END AS DEPOSIT_TYPE,

    CASE
        WHEN pm.MEMBER_ROLE = 'LEADER' THEN 20000
        ELSE 5000
    END AS DEPOSIT_AMOUNT,

    CASE
        WHEN pm.PARTY_MEMBER_ID % 4 = 0 THEN 'REFUNDED'
        WHEN pm.PARTY_MEMBER_ID % 4 = 1 THEN 'PAID'
        WHEN pm.PARTY_MEMBER_ID % 4 = 2 THEN 'PARTIAL_REFUNDED'
        ELSE 'PENDING'
    END AS DEPOSIT_STATUS,

    DATE_ADD(p.REG_DATE, INTERVAL 5 DAY) AS PAYMENT_DATE,

    CASE
        WHEN pm.PARTY_MEMBER_ID % 4 IN (0,2)
        THEN DATE_ADD(p.REG_DATE, INTERVAL 30 DAY)
    END AS REFUND_DATE,

    CASE
        WHEN pm.PARTY_MEMBER_ID % 4 = 0 THEN
            CASE WHEN pm.MEMBER_ROLE = 'LEADER' THEN 20000 ELSE 5000 END
        WHEN pm.PARTY_MEMBER_ID % 4 = 2 THEN
            CASE WHEN pm.MEMBER_ROLE = 'LEADER' THEN 10000 ELSE 2500 END
    END AS REFUND_AMOUNT,

    CONCAT('toss_dep_', LPAD(pm.PARTY_MEMBER_ID, 4, '0')) AS TOSS_PAYMENT_KEY,
    CONCAT('ORD_DEP_', LPAD(pm.PARTY_MEMBER_ID, 4, '0')) AS ORDER_ID

FROM PARTY_MEMBER pm
JOIN PARTY p
  ON p.PARTY_ID = pm.PARTY_ID
WHERE pm.PARTY_ID BETWEEN 6 AND 55
LIMIT 50;



-- TRANSFER_TRANSACTION: 입금이체 거래 기록
INSERT INTO TRANSFER_TRANSACTION (
    SETTLEMENT_ID,
    BANK_TRAN_ID,
    FINTECH_USE_NUM,
    TRAN_AMT,
    PRINT_CONTENT,
    REQ_CLIENT_NAME,
    RSP_CODE,
    RSP_MESSAGE,
    STATUS,
    CREATED_AT
)
SELECT
    s.SETTLEMENT_ID,

    CASE
        WHEN s.SETTLEMENT_ID % 3 <> 0
        THEN CONCAT(
            'T',
            DATE_FORMAT(s.SETTLEMENT_DATE, '%Y%m%d'),
            LPAD(s.SETTLEMENT_ID, 4, '0')
        )
        ELSE NULL
    END AS BANK_TRAN_ID,

    CONCAT(
        '100000000',
        LPAD(a.ACCOUNT_ID, 3, '0')
    ) AS FINTECH_USE_NUM,

    s.NET_AMOUNT AS TRAN_AMT,

    'MOA정산금' AS PRINT_CONTENT,

    a.ACCOUNT_HOLDER AS REQ_CLIENT_NAME,

    CASE
        WHEN s.SETTLEMENT_ID % 3 = 1 THEN '000'
        WHEN s.SETTLEMENT_ID % 3 = 2 THEN NULL
        ELSE '115'
    END AS RSP_CODE,

    CASE
        WHEN s.SETTLEMENT_ID % 3 = 1 THEN '정상처리'
        WHEN s.SETTLEMENT_ID % 3 = 2 THEN NULL
        ELSE '해당계좌없음'
    END AS RSP_MESSAGE,

    CASE
        WHEN s.SETTLEMENT_ID % 3 = 1 THEN 'SUCCESS'
        WHEN s.SETTLEMENT_ID % 3 = 2 THEN 'PENDING'
        ELSE 'FAILED'
    END AS STATUS,

    CASE
        WHEN s.SETTLEMENT_ID % 3 = 2
        THEN DATE_SUB(NOW(), INTERVAL 1 HOUR)
        ELSE s.SETTLEMENT_DATE
    END AS CREATED_AT
FROM SETTLEMENT s
JOIN ACCOUNT a
  ON a.ACCOUNT_ID = s.ACCOUNT_ID
WHERE NOT EXISTS (
    SELECT 1
    FROM TRANSFER_TRANSACTION t
    WHERE t.SETTLEMENT_ID = s.SETTLEMENT_ID
);


-- ============================================
-- 샘플 데이터 입력 완료
-- ============================================

SET FOREIGN_KEY_CHECKS = 1;