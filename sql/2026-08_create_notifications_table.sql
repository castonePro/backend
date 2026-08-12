-- Phase 3: 인앱 알림 이력 테이블
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 수동 실행.

CREATE TABLE IF NOT EXISTS notifications (
    notification_id UUID PRIMARY KEY,
    receiver_id      UUID NOT NULL REFERENCES users(user_id),
    title            VARCHAR(100) NOT NULL,
    body             TEXT NOT NULL,
    is_read          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_receiver ON notifications(receiver_id);
CREATE INDEX IF NOT EXISTS idx_notifications_receiver_unread ON notifications(receiver_id, is_read);
