-- 웹(브라우저) 푸시용 FCM 토큰
--
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 이 SQL을 먼저 실행하지 않으면 애플리케이션이 기동 단계에서
-- "Schema-validation: missing table [web_push_tokens]" 로 죽는다. 배포 전에 반드시 먼저 실행할 것.
--
-- 기존 fcm_tokens(모바일 앱, 사용자당 1개)는 그대로 두고 웹 토큰만 이 테이블에 따로 저장한다.
-- → 웹에 로그인해도 앱 토큰이 덮어써지지 않고, 한 사용자가 여러 브라우저에서 푸시를 받을 수 있다.
--
-- Oracle 기준. 재실행하면 ORA-00955(이름이 이미 사용 중)가 나는데 정상이니 무시하면 된다.
-- ※ 기존에 RAW(16)으로 생성된 경우 삭제 후 재생성:
-- DROP TABLE web_push_tokens CASCADE CONSTRAINTS;

CREATE TABLE web_push_tokens (
    token_id    VARCHAR2(36)   PRIMARY KEY,
    user_id     VARCHAR2(36)   NOT NULL,
    token       VARCHAR2(512)  NOT NULL,
    created_at  TIMESTAMP(6),
    updated_at  TIMESTAMP(6),
    CONSTRAINT uq_web_push_tokens_token UNIQUE (token),
    CONSTRAINT fk_web_push_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_web_push_tokens_user ON web_push_tokens(user_id);
