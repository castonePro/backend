-- Phase 2: 동행 그룹 채팅 메시지 테이블
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 수동 실행. 실행 순서: companion 테이블 생성 SQL 이후.

CREATE TABLE IF NOT EXISTS companion_chat_messages (
    message_id   UUID PRIMARY KEY,
    companion_id UUID NOT NULL REFERENCES companions(companion_id),
    sender_id    UUID NOT NULL REFERENCES users(user_id),
    content      TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_companion_chat_messages_companion ON companion_chat_messages(companion_id);
