-- Phase 1: 동행(companion) 도메인 테이블 생성
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 반드시 수동으로 실행할 것.
-- 실행 순서: 2026-08_add_phone_verification.sql 이후에 실행 (users 테이블 인증 필드 선행 필요)

-- UUID 기본값은 DB가 아닌 Hibernate(GenerationType.UUID)가 애플리케이션에서 채워 넣으므로
-- DEFAULT gen_random_uuid()를 걸지 않는다 (pgcrypto 확장 의존성도 피할 수 있음).
CREATE TABLE IF NOT EXISTS companions (
    companion_id      UUID PRIMARY KEY,
    itinerary_id      BIGINT NOT NULL REFERENCES itineraries(itinerary_id),
    host_id           UUID NOT NULL REFERENCES users(user_id),
    title             VARCHAR(100) NOT NULL,
    min_participants  INTEGER NOT NULL,
    max_participants  INTEGER NOT NULL,
    preference_tags   TEXT[],
    cost_sharing_note TEXT,
    description       TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
    start_date        DATE,
    end_date          DATE,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

-- 신청 이력을 보존하기 위해 취소/거절 시 행을 삭제하지 않는다(soft-cancel).
-- 그래서 (companion_id, applicant_id)에 유니크 제약을 걸지 않는다.
-- 활성 신청(PENDING/APPROVED) 중복 방지는 애플리케이션 레이어에서 처리한다.
CREATE TABLE IF NOT EXISTS companion_applications (
    application_id UUID PRIMARY KEY,
    companion_id   UUID NOT NULL REFERENCES companions(companion_id),
    applicant_id   UUID NOT NULL REFERENCES users(user_id),
    introduction   TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_companions_status ON companions(status);
CREATE INDEX IF NOT EXISTS idx_companions_host ON companions(host_id);
CREATE INDEX IF NOT EXISTS idx_companion_applications_companion ON companion_applications(companion_id);
CREATE INDEX IF NOT EXISTS idx_companion_applications_applicant ON companion_applications(applicant_id);
