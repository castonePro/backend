-- Phase 5: 신뢰·안전 (노쇼·신고·제재)
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 반드시 수동으로 실행할 것.
-- 실행 순서: 2026-08_create_companion_reviews.sql 이후에 실행

-- 1. users 테이블에 노쇼·제재 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS no_show_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS sanction_level VARCHAR(20) NOT NULL DEFAULT 'NONE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS restricted_until TIMESTAMP;

-- 2. companion_applications 테이블에 노쇼 처리 여부 컬럼 추가
ALTER TABLE companion_applications ADD COLUMN IF NOT EXISTS no_show BOOLEAN NOT NULL DEFAULT false;

-- 3. 신고 테이블
-- UUID 기본값은 DB가 아닌 Hibernate(GenerationType.UUID)가 애플리케이션에서 채워 넣으므로
-- DEFAULT gen_random_uuid()를 걸지 않는다.
CREATE TABLE IF NOT EXISTS reports (
    report_id        UUID PRIMARY KEY,
    reporter_id      UUID NOT NULL REFERENCES users(user_id),
    reported_user_id UUID NOT NULL REFERENCES users(user_id),
    companion_id     UUID REFERENCES companions(companion_id),
    reason_category  VARCHAR(30) NOT NULL,
    description       TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_reports_reported_user ON reports(reported_user_id);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);

