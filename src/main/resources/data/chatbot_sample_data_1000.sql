DROP PROCEDURE IF EXISTS seed_chatbot_knowledge;

DELIMITER //
CREATE PROCEDURE seed_chatbot_knowledge(IN p_count INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_category VARCHAR(20);
    DECLARE v_title VARCHAR(200);
    DECLARE v_question VARCHAR(500);
    DECLARE v_answer TEXT;
    DECLARE v_keywords VARCHAR(500);

    WHILE i <= p_count DO
        CASE (i - 1) % 7

        /* ===================== 구독 ===================== */
        WHEN 0 THEN
            SET v_category = '구독';
            SET v_title = CONCAT('구독 이용 안내 ', i);
            SET v_question = CONCAT('구독이 제대로 되지 않는 것 같아요. 어떻게 확인하나요? (문의번호 ', i, ')');
            SET v_answer =
'구독 이용이 원활하지 않아 불편하셨을 것 같아요.

먼저 마이페이지 > 내 구독에서 현재 구독 상태가 정상인지 확인해 주세요.
구독 상태가 정상으로 표시되는데도 서비스 이용이 어렵다면
잠시 후 다시 시도해 보시거나 앱/브라우저를 재실행해 보세요.

그래도 문제가 계속된다면 고객센터로 문의 주시면
확인 후 빠르게 도와드릴게요.';
            SET v_keywords = '구독,이용,상태확인,해지,변경';

        /* ===================== 결제 ===================== */
        WHEN 1 THEN
            SET v_category = '결제';
            SET v_title = CONCAT('결제 오류 안내 ', i);
            SET v_question = CONCAT('결제가 승인되지 않아요. 왜 그런가요? (문의번호 ', i, ')');
            SET v_answer =
'결제가 되지 않는 경우 카드 한도 부족이나 잔액 부족,
또는 카드사의 인터넷 결제 제한 때문일 수 있어요.

사용 중인 결제 수단의 상태를 한 번 확인해 보시고,
가능하다면 다른 카드나 결제 수단으로 다시 시도해 주세요.

여러 번 시도해도 결제가 되지 않는다면
고객센터로 문의 주시면 확인 후 안내해 드릴게요.';
            SET v_keywords = '결제,카드,승인실패,결제오류';

        /* ===================== 계정 ===================== */
        WHEN 2 THEN
            SET v_category = '계정';
            SET v_title = CONCAT('계정/로그인 도움말 ', i);
            SET v_question = CONCAT('비밀번호를 잊어버렸어요. 어떻게 해야 하나요? (문의번호 ', i, ')');
            SET v_answer =
'비밀번호가 기억나지 않아 당황하셨을 것 같아요.

로그인 화면에서 [비밀번호 재설정]을 선택한 후
가입하신 이메일 주소를 입력해 주세요.
이메일로 비밀번호를 다시 설정할 수 있는 안내를 보내드려요.

메일이 오지 않는다면 스팸함도 함께 확인해 주세요.';
            SET v_keywords = '계정,로그인,비밀번호,재설정';

        /* ===================== 파티 ===================== */
        WHEN 3 THEN
            SET v_category = '파티';
            SET v_title = CONCAT('파티 가입 안내 ', i);
            SET v_question = CONCAT('파티 가입 신청 후 언제 승인되나요? (문의번호 ', i, ')');
            SET v_answer =
'파티 가입 신청 후에는 파티장이 직접 승인 여부를 확인해요.

보통 신청 후 24시간 이내에 승인 또는 거절이 처리되며,
결과는 알림을 통해 안내해 드려요.

오랜 시간이 지나도 알림이 오지 않는다면
파티 상세 페이지에서 상태를 확인해 보세요.';
            SET v_keywords = '파티,가입,승인,파티장';

        /* ===================== 정산 ===================== */
        WHEN 4 THEN
            SET v_category = '정산';
            SET v_title = CONCAT('정산/보증금 안내 ', i);
            SET v_question = CONCAT('보증금과 정산 금액은 언제 돌려받나요? (문의번호 ', i, ')');
            SET v_answer =
'정산 일정이 궁금하셨을 것 같아요.

파티가 정상적으로 종료되면
종료일 기준으로 최대 7일 이내에
보증금과 마지막 정산 금액이 함께 환급돼요.

환급은 결제 시 사용하신 수단으로 자동 처리되며,
완료되면 알림을 통해 안내해 드려요.
7일이 지나도 환급이 되지 않았다면 고객센터로 문의해 주세요.';
            SET v_keywords = '정산,보증금,환불,지급';

        /* ===================== 오류 ===================== */
        WHEN 5 THEN
            SET v_category = '오류';
            SET v_title = CONCAT('서비스 오류 해결 방법 ', i);
            SET v_question = CONCAT('화면이 안 보이거나 흰 화면만 나와요. (문의번호 ', i, ')');
            SET v_answer =
'이용 중 화면이 정상적으로 보이지 않아 불편하셨을 것 같아요.

앱이나 브라우저를 완전히 종료한 뒤 다시 실행해 보시고,
네트워크 연결 상태도 함께 확인해 주세요.

문제가 계속 발생한다면
사용 중인 기기와 상황을 함께 적어 고객센터로 문의해 주세요.';
            SET v_keywords = '오류,버그,흰화면,로딩';

        /* ===================== 기타 ===================== */
        ELSE
            SET v_category = '기타';
            SET v_title = CONCAT('기타 문의 안내 ', i);
            SET v_question = CONCAT('서비스 이용 중 궁금한 점이 있어요. (문의번호 ', i, ')');
            SET v_answer =
'이용하시면서 궁금한 점이 생기셨군요.

문의하실 내용과 발생한 상황을 조금만 자세히 적어 주시면
더 정확하고 빠르게 안내해 드릴 수 있어요.

언제든 편하게 문의해 주세요.';
            SET v_keywords = '기타,문의,도움말';
        END CASE;

        INSERT INTO CHATBOT_KNOWLEDGE
            (category, title, question, answer, keywords)
        VALUES
            (v_category, v_title, v_question, v_answer, v_keywords);

        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

TRUNCATE TABLE CHATBOT_KNOWLEDGE;
CALL seed_chatbot_knowledge(1000);
