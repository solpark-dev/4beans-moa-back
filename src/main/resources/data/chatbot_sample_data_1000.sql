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
            WHEN 0 THEN
                SET v_category = '구독';
                SET v_title = CONCAT('구독 관련 안내 ', i);
                SET v_question = CONCAT('구독이 잘 안 되는데 어떻게 해야 하나요? (문의번호 ', i, ')');
                SET v_answer = '마이페이지 > 내 구독에서 현재 구독 상태를 확인하고, 문제가 계속되면 고객센터로 문의해 주세요.';
                SET v_keywords = '구독,신규구독,변경,해지';
            WHEN 1 THEN
                SET v_category = '결제';
                SET v_title = CONCAT('결제/카드 문의 ', i);
                SET v_question = CONCAT('결제 승인이 안 되는데 왜 그런가요? (문의번호 ', i, ')');
                SET v_answer = '결제 카드 한도, 잔액, 인터넷 결제 설정을 먼저 확인해 주세요. 그래도 안 되면 다른 카드나 결제 수단을 시도해 주세요.';
                SET v_keywords = '결제,카드,승인오류,결제실패';
            WHEN 2 THEN
                SET v_category = '계정';
                SET v_title = CONCAT('계정/로그인 문의 ', i);
                SET v_question = CONCAT('비밀번호를 잊어버렸어요. 어떻게 초기화하나요? (문의번호 ', i, ')');
                SET v_answer = '로그인 화면의 비밀번호 재설정 메뉴에서 이메일을 입력하면 재설정 링크를 보내드려요.';
                SET v_keywords = '계정,로그인,비밀번호,초기화';
            WHEN 3 THEN
                SET v_category = '파티';
                SET v_title = CONCAT('파티 참여/관리 안내 ', i);
                SET v_question = CONCAT('파티에 가입 신청했는데 언제 승인되나요? (문의번호 ', i, ')');
                SET v_answer = '파티장은 보통 24시간 이내에 가입 신청을 처리하며, 승인 여부는 알림으로 안내해 드려요.';
                SET v_keywords = '파티,가입,승인,파티장,파티원';
            WHEN 4 THEN
                SET v_category = '정산';
                SET v_title = CONCAT('정산/보증금 안내 ', i);
                SET v_question = CONCAT('보증금은 정산과 같이 언제 돌려받나요? (문의번호 ', i, ')');
                SET v_answer = '파티가 정상 종료되면 기준일로부터 7일 이내에 보증금과 정산 금액이 함께 처리돼요.';
                SET v_keywords = '정산,보증금,환불,지급,출금';
            WHEN 5 THEN
                SET v_category = '오류';
                SET v_title = CONCAT('앱/웹 오류 문의 ', i);
                SET v_question = CONCAT('화면이 안 열리거나 흰 화면만 보여요. (문의번호 ', i, ')');
                SET v_answer = '앱/브라우저를 완전히 종료 후 재실행하고, 네트워크 상태를 확인해 주세요. 그래도 반복되면 기기 정보와 함께 고객센터로 문의해 주세요.';
                SET v_keywords = '오류,버그,흰화면,로딩,에러';
            ELSE
                SET v_category = '기타';
                SET v_title = CONCAT('기타 서비스 이용 문의 ', i);
                SET v_question = CONCAT('서비스 사용 중 궁금한 점이 있어요. (문의번호 ', i, ')');
                SET v_answer = '궁금한 기능의 이름과 상황을 조금 더 자세히 적어 주시면 더 정확하게 안내해 드릴 수 있어요.';
                SET v_keywords = '기타,문의,사용방법,도움말';
        END CASE;

        INSERT INTO chatbot_knowledge
            (category, title, question, answer, keywords)
        VALUES (v_category, v_title, v_question, v_answer, v_keywords);

        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

TRUNCATE TABLE chatbot_knowledge;
CALL seed_chatbot_knowledge(1000);