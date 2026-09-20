-- AI 플래너 멀티턴 대화 세션
--
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 이 SQL을 먼저 실행하지 않으면 애플리케이션이 기동 단계에서
-- "Schema-validation: missing table [planner_sessions]" 로 죽는다. 반드시 먼저 실행할 것.
--
-- 아래는 실제 운영 DB인 Oracle 기준이다. (sql/ 폴더의 2026-08 파일들은
-- Oracle 마이그레이션 이전 PostgreSQL 시절 문법이라 참고만 할 것.)
-- Oracle에는 CREATE TABLE IF NOT EXISTS가 없으므로, 이미 만들어진 환경에서
-- 재실행하면 ORA-00955(이름이 이미 사용 중)가 난다. 정상이니 무시하면 된다.
--
-- 타입:
--   UUID -> VARCHAR2(36) (Hibernate @JdbcTypeCode(SqlTypes.VARCHAR))
--   String -> VARCHAR2,  @Lob String -> CLOB (@JdbcTypeCode(SqlTypes.CLOB))
--   Integer -> NUMBER(10),  LocalDateTime -> TIMESTAMP(6)
--
-- ※ 기존에 RAW(16)으로 생성되어 ORA-01465가 발생한 경우:
--   DROP TABLE planner_messages CASCADE CONSTRAINTS;
--   DROP TABLE planner_sessions CASCADE CONSTRAINTS;
--   실행 후 아래 CREATE TABLE을 다시 실행하세요.

CREATE TABLE planner_sessions (
    session_id         VARCHAR2(36)  PRIMARY KEY,
    user_id            VARCHAR2(36),                     -- 비로그인 세션이면 NULL
    locale             VARCHAR2(16),                     -- ko, en, ja, zh-CN, vi, id
    current_plan       CLOB,                             -- 현재 일정 JSON (single source of truth)
    preference_summary CLOB,                             -- 오래된 턴을 압축한 사용자 선호
    turn_count         NUMBER(10)    DEFAULT 0 NOT NULL,
    prompt_tokens      NUMBER(10)    DEFAULT 0 NOT NULL,
    completion_tokens  NUMBER(10)    DEFAULT 0 NOT NULL,
    created_at         TIMESTAMP(6),
    updated_at         TIMESTAMP(6)
);

CREATE INDEX idx_planner_sessions_user    ON planner_sessions(user_id);
CREATE INDEX idx_planner_sessions_updated ON planner_sessions(updated_at);

CREATE TABLE planner_messages (
    message_id        VARCHAR2(36)  PRIMARY KEY,
    session_id        VARCHAR2(36)  NOT NULL,
    seq               NUMBER(10)    NOT NULL,            -- 세션 내 순번 (created_at은 동률이 날 수 있음)
    role              VARCHAR2(20)  NOT NULL,            -- user | assistant
    content           CLOB          NOT NULL,            -- 자연어만. 일정 JSON은 넣지 않는다
    intent            VARCHAR2(20),                      -- NEW_PLAN | MODIFY | ASK | OUT_OF_SCOPE
    prompt_tokens     NUMBER(10),
    completion_tokens NUMBER(10),
    created_at        TIMESTAMP(6),
    CONSTRAINT fk_planner_messages_session
        FOREIGN KEY (session_id) REFERENCES planner_sessions(session_id) ON DELETE CASCADE
);

CREATE INDEX idx_planner_messages_session ON planner_messages(session_id, seq);


-- ─────────────────────────────────────────────────────────────────
-- PostgreSQL을 쓰는 환경이라면 위 대신 아래를 실행한다 (기록용).
--
-- CREATE TABLE IF NOT EXISTS planner_sessions (
--     session_id         UUID PRIMARY KEY,
--     user_id            UUID,
--     locale             VARCHAR(16),
--     current_plan       TEXT,
--     preference_summary TEXT,
--     turn_count         INTEGER NOT NULL DEFAULT 0,
--     prompt_tokens      INTEGER NOT NULL DEFAULT 0,
--     completion_tokens  INTEGER NOT NULL DEFAULT 0,
--     created_at         TIMESTAMP,
--     updated_at         TIMESTAMP
-- );
-- CREATE INDEX IF NOT EXISTS idx_planner_sessions_user    ON planner_sessions(user_id);
-- CREATE INDEX IF NOT EXISTS idx_planner_sessions_updated ON planner_sessions(updated_at);
--
-- CREATE TABLE IF NOT EXISTS planner_messages (
--     message_id        UUID PRIMARY KEY,
--     session_id        UUID NOT NULL REFERENCES planner_sessions(session_id) ON DELETE CASCADE,
--     seq               INTEGER NOT NULL,
--     role              VARCHAR(20) NOT NULL,
--     content           TEXT NOT NULL,
--     intent            VARCHAR(20),
--     prompt_tokens     INTEGER,
--     completion_tokens INTEGER,
--     created_at        TIMESTAMP
-- );
-- CREATE INDEX IF NOT EXISTS idx_planner_messages_session ON planner_messages(session_id, seq);
