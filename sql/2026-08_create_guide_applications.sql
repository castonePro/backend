-- Phase 6: 가이드 전환 심사
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 반드시 수동으로 실행할 것.
-- 실행 순서: 2026-08_create_reports_and_sanctions.sql 이후에 실행

-- 1. users 테이블에 예비 가이드 배지 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS preliminary_guide BOOLEAN NOT NULL DEFAULT false;

-- 2. 정식 가이드 전환 신청 테이블
-- UUID 기본값은 DB가 아닌 Hibernate(GenerationType.UUID)가 애플리케이션에서 채워 넣으므로
-- DEFAULT gen_random_uuid()를 걸지 않는다.
CREATE TABLE IF NOT EXISTS guide_applications (
    application_id UUID PRIMARY KEY,
    applicant_id    UUID NOT NULL REFERENCES users(user_id),
    message         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    applied_at      TIMESTAMP NOT NULL DEFAULT now(),
    reviewed_at     TIMESTAMP,
    review_note     TEXT
);

CREATE INDEX IF NOT EXISTS idx_guide_applications_applicant ON guide_applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_guide_applications_status ON guide_applications(status);
