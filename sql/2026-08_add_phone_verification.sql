-- Phase 0: 본인 인증 / 동행 프로필 필드 추가
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 반드시 수동으로 실행할 것.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_number   VARCHAR(30),
    ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS birth_year     INTEGER,
    ADD COLUMN IF NOT EXISTS gender         VARCHAR(10);
