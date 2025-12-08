-- ============================================
-- Payment API 테스트용 데이터 생성 스크립트
-- ============================================

-- 1. 기존 테스트 데이터 확인
SELECT 'USERS 테이블 확인' AS 'Step';
SELECT * FROM USERS WHERE USER_ID IN ('user_member1', 'user_member2', 'user_leader1');

SELECT 'PRODUCT 테이블 확인' AS 'Step';
SELECT * FROM PRODUCT LIMIT 5;

SELECT 'PARTY 테이블 확인' AS 'Step';
SELECT * FROM PARTY LIMIT 5;

SELECT 'PARTY_MEMBER 테이블 확인' AS 'Step';
SELECT * FROM PARTY_MEMBER LIMIT 5;

SELECT 'PAYMENT 테이블 확인' AS 'Step';
SELECT * FROM PAYMENT LIMIT 10;

-- ============================================
-- 테스트 데이터가 없는 경우 아래 스크립트 실행
-- ============================================

-- 2. 테스트용 사용자 생성 (이미 있으면 스킵)
INSERT IGNORE INTO USERS (USER_ID, PASSWORD, NICKNAME, PHONE, ROLE, USER_STATUS, REG_DATE, CI, DI, AGREE_MARKETING)
VALUES 
('user_leader1', 'password123', '방장1', '010-1111-1111', 'USER', 'ACTIVE', CURDATE(), 'CI_LEADER1', 'DI_LEADER1', 1),
('user_member1', 'password123', '멤버1', '010-2222-2222', 'USER', 'ACTIVE', CURDATE(), 'CI_MEMBER1', 'DI_MEMBER1', 1),
('user_member2', 'password123', '멤버2', '010-3333-3333', 'USER', 'ACTIVE', CURDATE(), 'CI_MEMBER2', 'DI_MEMBER2', 1);

-- 3. 테스트용 카테고리 생성
INSERT IGNORE INTO CATEGORY (CATEGORY_ID, CATEGORY_NAME)
VALUES (1, 'OTT');

-- 4. 테스트용 상품 생성
INSERT IGNORE INTO PRODUCT (PRODUCT_ID, CATEGORY_ID, PRODUCT_NAME, PRODUCT_STATUS, PRICE, IMAGE)
VALUES 
(1, 1, 'Netflix', 'ACTIVE', 13000, NULL),
(2, 1, 'Disney+', 'ACTIVE', 9900, NULL);

-- 5. 테스트용 파티 생성
INSERT IGNORE INTO PARTY (PARTY_ID, PRODUCT_ID, PARTY_LEADER_ID, PARTY_STATUS, MAX_MEMBERS, CURRENT_MEMBERS, MONTHLY_FEE, OTT_ID, OTT_PASSWORD, REG_DATE, START_DATE)
VALUES 
(1, 1, 'user_leader1', 'ACTIVE', 4, 3, 13000, 'netflix@test.com', 'password123', NOW(), DATE_ADD(CURDATE(), INTERVAL 1 DAY)),
(2, 2, 'user_leader1', 'RECRUITING', 4, 2, 9900, 'disney@test.com', 'password123', NOW(), DATE_ADD(CURDATE(), INTERVAL 2 DAY));

-- 6. 테스트용 파티 멤버 생성
INSERT IGNORE INTO PARTY_MEMBER (PARTY_MEMBER_ID, PARTY_ID, USER_ID, MEMBER_ROLE, MEMBER_STATUS, JOIN_DATE)
VALUES 
(1, 1, 'user_leader1', 'LEADER', 'ACTIVE', NOW()),
(2, 1, 'user_member1', 'MEMBER', 'ACTIVE', NOW()),
(3, 1, 'user_member2', 'MEMBER', 'ACTIVE', NOW()),
(4, 2, 'user_leader1', 'LEADER', 'ACTIVE', NOW()),
(5, 2, 'user_member1', 'MEMBER', 'ACTIVE', NOW());

-- 7. 테스트용 결제 데이터 생성
INSERT IGNORE INTO PAYMENT (PAYMENT_ID, PARTY_ID, PARTY_MEMBER_ID, USER_ID, PAYMENT_TYPE, PAYMENT_AMOUNT, PAYMENT_STATUS, PAYMENT_METHOD, PAYMENT_DATE, TARGET_MONTH, TOSS_PAYMENT_KEY, ORDER_ID)
VALUES 
-- 파티 1 결제 내역
(1, 1, 2, 'user_member1', 'INITIAL', 3250, 'COMPLETED', 'CARD', NOW(), DATE_FORMAT(NOW(), '%Y-%m'), 'test_payment_key_001', 'ORDER_001'),
(2, 1, 3, 'user_member2', 'INITIAL', 3250, 'COMPLETED', 'CARD', NOW(), DATE_FORMAT(NOW(), '%Y-%m'), 'test_payment_key_002', 'ORDER_002'),
(3, 1, 2, 'user_member1', 'MONTHLY', 3250, 'COMPLETED', 'CARD', DATE_ADD(NOW(), INTERVAL 1 MONTH), DATE_FORMAT(DATE_ADD(NOW(), INTERVAL 1 MONTH), '%Y-%m'), 'test_payment_key_003', 'ORDER_003'),
(4, 1, 3, 'user_member2', 'MONTHLY', 3250, 'COMPLETED', 'CARD', DATE_ADD(NOW(), INTERVAL 1 MONTH), DATE_FORMAT(DATE_ADD(NOW(), INTERVAL 1 MONTH), '%Y-%m'), 'test_payment_key_004', 'ORDER_004'),

-- 파티 2 결제 내역
(5, 2, 5, 'user_member1', 'INITIAL', 2475, 'COMPLETED', 'CARD', NOW(), DATE_FORMAT(NOW(), '%Y-%m'), 'test_payment_key_005', 'ORDER_005');

-- 8. 결과 확인
SELECT '=== 생성된 테스트 데이터 확인 ===' AS 'Result';

SELECT 'PAYMENT 테이블' AS 'Table', COUNT(*) AS 'Count' FROM PAYMENT;
SELECT * FROM PAYMENT ORDER BY PAYMENT_ID DESC LIMIT 10;

SELECT 'PARTY 테이블' AS 'Table', COUNT(*) AS 'Count' FROM PARTY;
SELECT * FROM PARTY ORDER BY PARTY_ID DESC LIMIT 5;

SELECT 'PARTY_MEMBER 테이블' AS 'Table', COUNT(*) AS 'Count' FROM PARTY_MEMBER;
SELECT * FROM PARTY_MEMBER ORDER BY PARTY_MEMBER_ID DESC LIMIT 10;
