# MOA 구독 관리 시스템 (MOA Subscription Management System)

MOA는 사용자가 구독 서비스를 효율적으로 관리하고, 파티를 맺어 비용을 절약하며, 정산 및 결제를 편리하게 이용할 수 있도록 돕는 백엔드 시스템입니다.

## 🛠 기술 스택 (Tech Stack)

*   **Java**: 17
*   **Spring Boot**: 3.5.8
*   **Database**: MySQL, MyBatis 3.0.3
*   **Security**: Spring Security, JWT, OAuth 2.0 (Kakao, Google), TOTP
*   **Build Tool**: Maven
*   **External APIs**:
    *   Resend (Email)

## ✨ 주요 기능 (Key Features)

*   **구독 관리 (Subscription Management)**: 구독 상품 관리, 구독 내역 조회 및 변경
*   **파티 관리 (Party Management)**: 구독 공유를 위한 파티 생성, 초대, 참여
*   **정산 및 결제 (Settlement & Payment)**:
    *   파티원 간 자동 정산
    *   카드/계좌 결제 관리
*   **사용자 관리 (User Management)**:
    *   회원가입, 로그인 (일반, OAuth)
    *   로그인 이력 관리
    *   마이페이지 (프로필, 보안 설정)
*   **커뮤니티 (Community)**:
    *   공지사항, FAQ, 1:1 문의

## 📋 사전 준비 사항 (Prerequisites)

이 프로젝트를 실행하기 위해서는 다음 환경이 구성되어 있어야 합니다.

*   JDK 17 이상
*   MySQL 데이터베이스
*   Maven (프로젝트 내 `mvnw` 포함)

## ⚙️ 설치 및 설정 (Setup & Configuration)

### 1. 데이터베이스 설정

MySQL에 `moa` 데이터베이스를 생성하고, 제공된 SQL 파일을 실행하여 스키마와 샘플 데이터를 초기화합니다.

```bash
# 데이터베이스 생성
CREATE DATABASE moa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

`src/main/resources` 디렉토리 내의 다음 파일들을 순서대로 실행하세요.
1.  `moa_schema_20251211_1.sql` (테이블 생성)
2.  `sample-data2_20251211_1.sql` (샘플 데이터 삽입)

### 2. 환경 변수 설정 (application-secret.properties)

프로젝트 루트 또는 `src/main/resources` 위치에 `application-secret.properties` 파일을 생성하고, 아래의 필수 설정값들을 채워주세요.

```properties
# 데이터베이스 설정
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD

# JWT 시크릿 키 (32자 이상)
jwt.secret=YOUR_JWT_SECRET_KEY_MUST_BE_VERY_LONG

# 외부 API 키 설정
resend.api-key=YOUR_RESEND_API_KEY

# OAuth 설정
oauth.kakao.client-id=YOUR_KAKAO_CLIENT_ID
oauth.google.client-id=YOUR_GOOGLE_CLIENT_ID
oauth.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
```

## 🚀 실행 방법 (Running the Application)

프로젝트 루트 디렉토리에서 다음 명령어를 실행하여 애플리케이션을 시작합니다.

```bash
./mvnw spring-boot:run
```

실행 후 브라우저에서 `https://localhost:8443` (기본 설정)으로 접속할 수 있습니다.
*주의: `application-local.properties`에 SSL 설정이 되어 있습니다.*

## 📚 API 문서 (API Documentation)

서버가 실행되면 다양한 REST API를 사용할 수 있습니다. 주요 컨트롤러는 다음과 같습니다.

*   `/api/auth`: 인증 (로그인, 토큰 갱신)
*   `/api/user`: 사용자 정보 관리
*   `/api/subscription`: 구독 상품 및 내역
*   `/api/party`: 파티 관리
*   `/api/settlement`: 정산 관련
