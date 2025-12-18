-- ============================================
-- MOA 파티 및 파티 멤버 더미 데이터
-- PARTY (ID 6~55), PARTY_MEMBER
-- ============================================

/* --------------------------------------------
 * 1. PARTY 테이블 데이터 (50건)
 * -------------------------------------------- */
INSERT INTO PARTY (
    PARTY_ID, PRODUCT_ID, PARTY_LEADER_ID,
    PARTY_STATUS, MAX_MEMBERS, CURRENT_MEMBERS,
    MONTHLY_FEE, OTT_ID, OTT_PASSWORD,
    ACCOUNT_ID, REG_DATE, START_DATE, END_DATE
)
SELECT
    5 + seq AS PARTY_ID,
    (seq % 21) + 1 AS PRODUCT_ID,
    u.USER_ID AS PARTY_LEADER_ID,

    CASE
        WHEN seq % 4 = 0 THEN 'ACTIVE'
        WHEN seq % 4 = 1 THEN 'RECRUITING'
        WHEN seq % 4 = 2 THEN 'PENDING_PAYMENT'
        ELSE 'EXPIRED'
    END AS PARTY_STATUS,

    4 AS MAX_MEMBERS,

    CASE
        WHEN seq % 4 = 0 THEN 4
        WHEN seq % 4 = 1 THEN 2
        ELSE 1
    END AS CURRENT_MEMBERS,

    (seq % 10 + 1) * 750 AS MONTHLY_FEE,

    CASE WHEN seq % 4 = 0 THEN CONCAT('moa_ott_', 5 + seq) END AS OTT_ID,
    CASE WHEN seq % 4 = 0 THEN 'safe_pass!' END AS OTT_PASSWORD,

    (seq % 20) + 1 AS ACCOUNT_ID,

    DATE_ADD('2024-11-01', INTERVAL seq DAY) AS REG_DATE,
    DATE_ADD('2024-11-03', INTERVAL seq DAY) AS START_DATE,

    CASE
        WHEN seq % 4 = 3
        THEN DATE_ADD('2024-12-01', INTERVAL seq DAY)
    END AS END_DATE
FROM (
    SELECT @n := @n + 1 AS seq
    FROM INFORMATION_SCHEMA.COLUMNS, (SELECT @n := 0) r
    LIMIT 50
) t
JOIN (
    SELECT USER_ID,
           ROW_NUMBER() OVER () - 1 AS rn
    FROM USERS
    WHERE ROLE = 'USER'
    ORDER BY USER_ID
    LIMIT 20
) u
ON (t.seq % 20) = u.rn;


/* --------------------------------------------
 * 2. PARTY_MEMBER : 파티 리더 (50명)
 * -------------------------------------------- */
INSERT INTO PARTY_MEMBER (
    PARTY_ID,
    USER_ID,
    MEMBER_ROLE,
    MEMBER_STATUS,
    JOIN_DATE
)
SELECT
    p.PARTY_ID,
    p.PARTY_LEADER_ID,
    'LEADER',
    'ACTIVE',
    p.REG_DATE
FROM PARTY p
WHERE p.PARTY_ID BETWEEN 6 AND 55;


/* --------------------------------------------
 * 3. PARTY_MEMBER : 일반 멤버
 * -------------------------------------------- */
INSERT INTO PARTY_MEMBER (
    PARTY_ID,
    USER_ID,
    MEMBER_ROLE,
    MEMBER_STATUS,
    JOIN_DATE
)
SELECT
    p.PARTY_ID,
    u.USER_ID,
    'MEMBER',
    CASE
        WHEN p.PARTY_STATUS = 'ACTIVE' THEN 'ACTIVE'
        ELSE 'PENDING_PAYMENT'
    END AS MEMBER_STATUS,
    DATE_ADD(p.REG_DATE, INTERVAL 1 DAY) AS JOIN_DATE
FROM PARTY p
JOIN USERS u
  ON u.USER_ID <> p.PARTY_LEADER_ID
WHERE p.PARTY_ID BETWEEN 6 AND 55
  AND p.CURRENT_MEMBERS > 1
LIMIT 80;
