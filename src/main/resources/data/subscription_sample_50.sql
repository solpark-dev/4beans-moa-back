-- ============================================
-- SUBSCRIPTION (구독 정보) 신규 샘플 데이터 50개
-- SUBSCRIPTION_ID: 21~70
-- 다양한 상태: ACTIVE, CANCELLED, EXPIRED
-- ============================================

INSERT INTO SUBSCRIPTION (
    USER_ID, PRODUCT_ID, SUBSCRIPTION_STATUS,
    START_DATE, END_DATE, CANCEL_REASON, CANCEL_DATE
) VALUES
-- 현재 활성 구독
('user001@gmail.com', 2, 'ACTIVE', '2024-07-01', NULL, NULL, NULL),
('user001@gmail.com', 3, 'ACTIVE', '2024-08-01', NULL, NULL, NULL),

('user002@naver.com', 4, 'ACTIVE', '2024-07-05', NULL, NULL, NULL),
('user002@naver.com', 5, 'ACTIVE', '2024-08-05', NULL, NULL, NULL),

('user003@daum.net', 7, 'ACTIVE', '2024-07-10', NULL, NULL, NULL),
('user003@daum.net', 8, 'ACTIVE', '2024-08-10', NULL, NULL, NULL),

('user004@gmail.com', 8, 'ACTIVE', '2024-07-15', NULL, NULL, NULL),
('user004@gmail.com', 1, 'ACTIVE', '2024-08-15', NULL, NULL, NULL),

('user005@naver.com', 4, 'ACTIVE', '2024-07-20', NULL, NULL, NULL),
('user005@naver.com', 5, 'ACTIVE', '2024-08-20', NULL, NULL, NULL),

('user006@daum.net', 1, 'ACTIVE', '2024-07-25', NULL, NULL, NULL),
('user006@daum.net', 2, 'ACTIVE', '2024-08-25', NULL, NULL, NULL),

('user007@gmail.com', 3, 'ACTIVE', '2024-07-01', NULL, NULL, NULL),
('user007@gmail.com', 7, 'ACTIVE', '2024-08-01', NULL, NULL, NULL),

('user008@naver.com', 7, 'ACTIVE', '2024-07-05', NULL, NULL, NULL),
('user008@naver.com', 8, 'ACTIVE', '2024-08-05', NULL, NULL, NULL),

('user009@daum.net', 1, 'ACTIVE', '2024-07-10', NULL, NULL, NULL),
('user009@daum.net', 4, 'ACTIVE', '2024-08-10', NULL, NULL, NULL),

('user010@gmail.com', 2, 'ACTIVE', '2024-07-15', NULL, NULL, NULL),
('user010@gmail.com', 3, 'ACTIVE', '2024-08-15', NULL, NULL, NULL),

('user011@naver.com', 5, 'ACTIVE', '2024-07-20', NULL, NULL, NULL),
('user011@naver.com', 7, 'ACTIVE', '2024-08-20', NULL, NULL, NULL),

('user012@daum.net', 8, 'ACTIVE', '2024-07-25', NULL, NULL, NULL),
('user012@daum.net', 1, 'ACTIVE', '2024-08-25', NULL, NULL, NULL),

('user013@gmail.com', 4, 'ACTIVE', '2024-07-01', NULL, NULL, NULL),
('user013@gmail.com', 7, 'ACTIVE', '2024-08-01', NULL, NULL, NULL),

('user014@naver.com', 2, 'ACTIVE', '2024-07-05', NULL, NULL, NULL),
('user014@naver.com', 5, 'ACTIVE', '2024-08-05', NULL, NULL, NULL),

('user015@daum.net', 3, 'ACTIVE', '2024-07-10', NULL, NULL, NULL),
('user015@daum.net', 7, 'ACTIVE', '2024-08-10', NULL, NULL, NULL),

('user016@gmail.com', 1, 'ACTIVE', '2024-07-15', NULL, NULL, NULL),
('user016@gmail.com', 4, 'ACTIVE', '2024-08-15', NULL, NULL, NULL),

('user017@naver.com', 2, 'ACTIVE', '2024-07-20', NULL, NULL, NULL),
('user017@naver.com', 7, 'ACTIVE', '2024-08-20', NULL, NULL, NULL),

('user018@daum.net', 5, 'ACTIVE', '2024-07-25', NULL, NULL, NULL),

-- 취소된 구독
('user019@gmail.com', 3, 'CANCELLED', '2024-05-01', '2024-08-01', '서비스 불만족', '2024-07-25'),
('user019@gmail.com', 8, 'CANCELLED', '2024-05-10', '2024-08-10', '경제적 부담', '2024-08-05'),

('user020@naver.com', 1, 'CANCELLED', '2024-05-15', '2024-08-15', '타 서비스 이용', '2024-08-10'),
('user020@naver.com', 4, 'CANCELLED', '2024-05-20', '2024-08-20', '이용 빈도 낮음', '2024-08-15'),

('user001@gmail.com', 7, 'CANCELLED', '2024-05-25', '2024-08-25', '콘텐츠 부족', '2024-08-20'),

('user002@naver.com', 7, 'CANCELLED', '2024-06-01', '2024-09-01', '서비스 불만족', '2024-08-25'),

('user003@daum.net', 1, 'CANCELLED', '2024-06-05', '2024-09-05', '경제적 부담', '2024-09-01'),

('user004@gmail.com', 5, 'CANCELLED', '2024-06-10', '2024-09-10', '타 서비스 이용', '2024-09-05'),

('user005@naver.com', 2, 'CANCELLED', '2024-06-15', '2024-09-15', '이용 빈도 낮음', '2024-09-10'),

('user006@daum.net', 4, 'CANCELLED', '2024-06-20', '2024-09-20', '파티장 문제', '2024-09-15'),

-- 만료된 구독
('user007@gmail.com', 8, 'EXPIRED', '2024-04-01', '2024-07-01', NULL, NULL),
('user008@naver.com', 1, 'EXPIRED', '2024-04-05', '2024-07-05', NULL, NULL),
('user009@daum.net', 3, 'EXPIRED', '2024-04-10', '2024-07-10', NULL, NULL),
('user010@gmail.com', 7, 'EXPIRED', '2024-04-15', '2024-07-15', NULL, NULL),
('user011@naver.com', 2, 'EXPIRED', '2024-04-20', '2024-07-20', NULL, NULL);
