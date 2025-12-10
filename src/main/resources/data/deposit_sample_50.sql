-- ============================================
-- DEPOSIT (보증금) 신규 샘플 데이터 50개
-- DEPOSIT_ID: 21~70
-- 기존 파티(1~5) 및 파티멤버(1~20) 재활용
-- 다양한 상태: PAID, REFUNDED, PARTIAL_REFUNDED, PENDING
-- ============================================

INSERT INTO DEPOSIT (
    DEPOSIT_ID, PARTY_ID, PARTY_MEMBER_ID, USER_ID, DEPOSIT_TYPE,
    DEPOSIT_AMOUNT, DEPOSIT_STATUS, PAYMENT_DATE, REFUND_DATE, REFUND_AMOUNT,
    TOSS_PAYMENT_KEY, ORDER_ID
) VALUES
-- 파티 1 추가 보증금 (10개)
(21, 1, 1, 'user001@gmail.com', 'LEADER', 17000, 'PAID', '2024-07-01 10:05:00', NULL, NULL, 'toss_dep_021', 'ORD_DEP_021'),
(22, 1, 2, 'user002@naver.com', 'SECURITY', 4250, 'PAID', '2024-07-02 10:05:00', NULL, NULL, 'toss_dep_022', 'ORD_DEP_022'),
(23, 1, 3, 'user011@naver.com', 'SECURITY', 4250, 'REFUNDED', '2024-08-01 10:05:00', '2024-11-05 10:00:00', 4250, 'toss_dep_023', 'ORD_DEP_023'),
(24, 1, 4, 'user012@daum.net', 'SECURITY', 4250, 'PARTIAL_REFUNDED', '2024-08-02 10:05:00', '2024-10-15 10:00:00', 2125, 'toss_dep_024', 'ORD_DEP_024'),
(25, 1, 1, 'user001@gmail.com', 'LEADER', 17000, 'PAID', '2024-09-01 10:05:00', NULL, NULL, 'toss_dep_025', 'ORD_DEP_025'),
(26, 1, 2, 'user002@naver.com', 'SECURITY', 4250, 'PAID', '2024-09-02 10:05:00', NULL, NULL, 'toss_dep_026', 'ORD_DEP_026'),
(27, 1, 3, 'user011@naver.com', 'SECURITY', 4250, 'PAID', '2024-10-01 10:05:00', NULL, NULL, 'toss_dep_027', 'ORD_DEP_027'),
(28, 1, 4, 'user012@daum.net', 'SECURITY', 4250, 'PENDING', '2024-11-01 10:05:00', NULL, NULL, 'toss_dep_028', 'ORD_DEP_028'),
(29, 1, 1, 'user001@gmail.com', 'LEADER', 17000, 'PAID', '2024-11-05 10:05:00', NULL, NULL, 'toss_dep_029', 'ORD_DEP_029'),
(30, 1, 2, 'user002@naver.com', 'SECURITY', 4250, 'PAID', '2024-11-06 10:05:00', NULL, NULL, 'toss_dep_030', 'ORD_DEP_030'),

-- 파티 2 추가 보증금 (10개)
(31, 2, 5, 'user003@daum.net', 'LEADER', 10900, 'PAID', '2024-07-10 10:05:00', NULL, NULL, 'toss_dep_031', 'ORD_DEP_031'),
(32, 2, 6, 'user004@gmail.com', 'SECURITY', 2725, 'PAID', '2024-07-11 10:05:00', NULL, NULL, 'toss_dep_032', 'ORD_DEP_032'),
(33, 2, 7, 'user013@gmail.com', 'SECURITY', 2725, 'REFUNDED', '2024-08-10 10:05:00', '2024-11-15 10:00:00', 2725, 'toss_dep_033', 'ORD_DEP_033'),
(34, 2, 8, 'user014@naver.com', 'SECURITY', 2725, 'PAID', '2024-08-11 10:05:00', NULL, NULL, 'toss_dep_034', 'ORD_DEP_034'),
(35, 2, 5, 'user003@daum.net', 'LEADER', 10900, 'PAID', '2024-09-10 10:05:00', NULL, NULL, 'toss_dep_035', 'ORD_DEP_035'),
(36, 2, 6, 'user004@gmail.com', 'SECURITY', 2725, 'PARTIAL_REFUNDED', '2024-09-11 10:05:00', '2024-11-20 10:00:00', 1363, 'toss_dep_036', 'ORD_DEP_036'),
(37, 2, 7, 'user013@gmail.com', 'SECURITY', 2725, 'PAID', '2024-10-10 10:05:00', NULL, NULL, 'toss_dep_037', 'ORD_DEP_037'),
(38, 2, 8, 'user014@naver.com', 'SECURITY', 2725, 'PAID', '2024-10-11 10:05:00', NULL, NULL, 'toss_dep_038', 'ORD_DEP_038'),
(39, 2, 5, 'user003@daum.net', 'LEADER', 10900, 'PENDING', '2024-11-10 10:05:00', NULL, NULL, 'toss_dep_039', 'ORD_DEP_039'),
(40, 2, 6, 'user004@gmail.com', 'SECURITY', 2725, 'PAID', '2024-11-11 10:05:00', NULL, NULL, 'toss_dep_040', 'ORD_DEP_040'),

-- 파티 3 추가 보증금 (10개)
(41, 3, 9, 'user005@naver.com', 'LEADER', 7900, 'PAID', '2024-07-20 10:05:00', NULL, NULL, 'toss_dep_041', 'ORD_DEP_041'),
(42, 3, 10, 'user006@daum.net', 'SECURITY', 1975, 'PAID', '2024-07-21 10:05:00', NULL, NULL, 'toss_dep_042', 'ORD_DEP_042'),
(43, 3, 11, 'user015@daum.net', 'SECURITY', 1975, 'PAID', '2024-07-22 10:05:00', NULL, NULL, 'toss_dep_043', 'ORD_DEP_043'),
(44, 3, 12, 'user016@gmail.com', 'SECURITY', 1975, 'REFUNDED', '2024-08-20 10:05:00', '2024-11-25 10:00:00', 1975, 'toss_dep_044', 'ORD_DEP_044'),
(45, 3, 9, 'user005@naver.com', 'LEADER', 7900, 'PAID', '2024-09-20 10:05:00', NULL, NULL, 'toss_dep_045', 'ORD_DEP_045'),
(46, 3, 10, 'user006@daum.net', 'SECURITY', 1975, 'PAID', '2024-09-21 10:05:00', NULL, NULL, 'toss_dep_046', 'ORD_DEP_046'),
(47, 3, 11, 'user015@daum.net', 'SECURITY', 1975, 'PARTIAL_REFUNDED', '2024-10-20 10:05:00', '2024-11-30 10:00:00', 988, 'toss_dep_047', 'ORD_DEP_047'),
(48, 3, 12, 'user016@gmail.com', 'SECURITY', 1975, 'PAID', '2024-10-21 10:05:00', NULL, NULL, 'toss_dep_048', 'ORD_DEP_048'),
(49, 3, 9, 'user005@naver.com', 'LEADER', 7900, 'PAID', '2024-11-20 10:05:00', NULL, NULL, 'toss_dep_049', 'ORD_DEP_049'),
(50, 3, 10, 'user006@daum.net', 'SECURITY', 1975, 'PENDING', '2024-11-21 10:05:00', NULL, NULL, 'toss_dep_050', 'ORD_DEP_050'),

-- 파티 4 추가 보증금 (10개)
(51, 4, 13, 'user007@gmail.com', 'LEADER', 13900, 'PAID', '2024-07-01 10:05:00', NULL, NULL, 'toss_dep_051', 'ORD_DEP_051'),
(52, 4, 14, 'user008@naver.com', 'SECURITY', 3475, 'PAID', '2024-07-02 10:05:00', NULL, NULL, 'toss_dep_052', 'ORD_DEP_052'),
(53, 4, 15, 'user017@naver.com', 'SECURITY', 3475, 'PAID', '2024-07-03 10:05:00', NULL, NULL, 'toss_dep_053', 'ORD_DEP_053'),
(54, 4, 16, 'user018@daum.net', 'SECURITY', 3475, 'REFUNDED', '2024-08-01 10:05:00', '2024-11-05 10:00:00', 3475, 'toss_dep_054', 'ORD_DEP_054'),
(55, 4, 13, 'user007@gmail.com', 'LEADER', 13900, 'PAID', '2024-09-01 10:05:00', NULL, NULL, 'toss_dep_055', 'ORD_DEP_055'),
(56, 4, 14, 'user008@naver.com', 'SECURITY', 3475, 'PARTIAL_REFUNDED', '2024-09-02 10:05:00', '2024-10-10 10:00:00', 1738, 'toss_dep_056', 'ORD_DEP_056'),
(57, 4, 15, 'user017@naver.com', 'SECURITY', 3475, 'PAID', '2024-10-01 10:05:00', NULL, NULL, 'toss_dep_057', 'ORD_DEP_057'),
(58, 4, 16, 'user018@daum.net', 'SECURITY', 3475, 'PAID', '2024-10-02 10:05:00', NULL, NULL, 'toss_dep_058', 'ORD_DEP_058'),
(59, 4, 13, 'user007@gmail.com', 'LEADER', 13900, 'PAID', '2024-11-01 10:05:00', NULL, NULL, 'toss_dep_059', 'ORD_DEP_059'),
(60, 4, 14, 'user008@naver.com', 'SECURITY', 3475, 'PENDING', '2024-11-02 10:05:00', NULL, NULL, 'toss_dep_060', 'ORD_DEP_060'),

-- 파티 5 추가 보증금 (10개)
(61, 5, 17, 'user009@daum.net', 'LEADER', 29000, 'PAID', '2024-07-10 10:05:00', NULL, NULL, 'toss_dep_061', 'ORD_DEP_061'),
(62, 5, 18, 'user010@gmail.com', 'SECURITY', 7250, 'PAID', '2024-07-11 10:05:00', NULL, NULL, 'toss_dep_062', 'ORD_DEP_062'),
(63, 5, 19, 'user019@gmail.com', 'SECURITY', 7250, 'PAID', '2024-07-12 10:05:00', NULL, NULL, 'toss_dep_063', 'ORD_DEP_063'),
(64, 5, 20, 'user020@naver.com', 'SECURITY', 7250, 'REFUNDED', '2024-08-10 10:05:00', '2024-11-15 10:00:00', 7250, 'toss_dep_064', 'ORD_DEP_064'),
(65, 5, 17, 'user009@daum.net', 'LEADER', 29000, 'PAID', '2024-09-10 10:05:00', NULL, NULL, 'toss_dep_065', 'ORD_DEP_065'),
(66, 5, 18, 'user010@gmail.com', 'SECURITY', 7250, 'PAID', '2024-09-11 10:05:00', NULL, NULL, 'toss_dep_066', 'ORD_DEP_066'),
(67, 5, 19, 'user019@gmail.com', 'SECURITY', 7250, 'PARTIAL_REFUNDED', '2024-10-10 10:05:00', '2024-11-20 10:00:00', 3625, 'toss_dep_067', 'ORD_DEP_067'),
(68, 5, 20, 'user020@naver.com', 'SECURITY', 7250, 'PAID', '2024-10-11 10:05:00', NULL, NULL, 'toss_dep_068', 'ORD_DEP_068'),
(69, 5, 17, 'user009@daum.net', 'LEADER', 29000, 'PAID', '2024-11-10 10:05:00', NULL, NULL, 'toss_dep_069', 'ORD_DEP_069'),
(70, 5, 18, 'user010@gmail.com', 'SECURITY', 7250, 'PENDING', '2024-11-11 10:05:00', NULL, NULL, 'toss_dep_070', 'ORD_DEP_070');
