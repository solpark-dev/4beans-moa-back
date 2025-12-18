-- 1. PAYMENT_ID 시작값 세팅
SET @pid := 20;

-- 2. PAYMENT 더미 생성
INSERT INTO PAYMENT (
    PAYMENT_ID,
    PARTY_ID,
    PARTY_MEMBER_ID,
    USER_ID,
    PAYMENT_TYPE,
    PAYMENT_AMOUNT,
    PAYMENT_STATUS,
    PAYMENT_METHOD,
    PAYMENT_DATE,
    TOSS_PAYMENT_KEY,
    ORDER_ID,
    CARD_NUMBER,
    CARD_COMPANY,
    TARGET_MONTH
)
SELECT
    @pid := @pid + 1                                   AS PAYMENT_ID,
    pm.PARTY_ID,
    pm.PARTY_MEMBER_ID,
    pm.USER_ID,
    'MONTHLY'                                          AS PAYMENT_TYPE,
    CASE pm.PARTY_ID
        WHEN 6 THEN 4250
        WHEN 7 THEN 4250
        WHEN 8 THEN 2725
        WHEN 9 THEN 2725
        WHEN 10 THEN 1975
        WHEN 11 THEN 1975
        WHEN 12 THEN 3475
        WHEN 13 THEN 3475
        ELSE 3000
    END                                                AS PAYMENT_AMOUNT,
    CASE
        WHEN @pid % 7 = 0 THEN 'FAILED'
        WHEN @pid % 5 = 0 THEN 'PENDING'
        ELSE 'COMPLETED'
    END                                                AS PAYMENT_STATUS,
    'CARD'                                             AS PAYMENT_METHOD,
    CONCAT(m.ym, '-01 00:05:00')                        AS PAYMENT_DATE,
    CONCAT('toss_pk_', LPAD(@pid, 3, '0'))              AS TOSS_PAYMENT_KEY,
    CONCAT('ORD_', REPLACE(m.ym, '-', ''), '_',
           LPAD(@pid, 3, '0'))                          AS ORDER_ID,
    '****-****-****-****'                               AS CARD_NUMBER,
    'MOA_CARD'                                         AS CARD_COMPANY,
    m.ym                                               AS TARGET_MONTH
FROM PARTY_MEMBER pm
JOIN (
    SELECT '2024-07' AS ym UNION ALL
    SELECT '2024-08' UNION ALL
    SELECT '2024-09' UNION ALL
    SELECT '2024-10' UNION ALL
    SELECT '2024-11'
) m
WHERE pm.PARTY_ID BETWEEN 6 AND 13
ORDER BY m.ym, pm.PARTY_ID, pm.PARTY_MEMBER_ID;
