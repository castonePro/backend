-- Phase 4: 여행 완료·평가·이력
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 반드시 수동으로 실행할 것.
-- 실행 순서: 2026-08_create_companion_tables.sql 이후에 실행 (companions 테이블 선행 필요)

-- 1. users 테이블에 동행 이력 카운트 컬럼 추가 (Phase 6 가이드 전환 심사의 기초 자료)
ALTER TABLE users ADD COLUMN IF NOT EXISTS companion_host_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS companion_join_count INTEGER NOT NULL DEFAULT 0;

-- 2. companions 테이블에 완료 시각 컬럼 추가 (더블 블라인드 리뷰 공개 기한 7일 계산 기준)
ALTER TABLE companions ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

-- 3. 동행 상호 평가 테이블
-- UUID 기본값은 DB가 아닌 Hibernate(GenerationType.UUID)가 애플리케이션에서 채워 넣으므로
-- DEFAULT gen_random_uuid()를 걸지 않는다.
-- 더블 블라인드 공개 여부는 저장하지 않고, (쌍방 제출 여부 / completed_at + 7일 경과)를
-- 애플리케이션(CompanionReviewService)에서 조회 시점에 계산한다.
CREATE TABLE IF NOT EXISTS companion_reviews (
    review_id        UUID PRIMARY KEY,
    companion_id     UUID NOT NULL REFERENCES companions(companion_id),
    reviewer_id      UUID NOT NULL REFERENCES users(user_id),
    reviewee_id      UUID NOT NULL REFERENCES users(user_id),
    rating           NUMERIC(2,1) NOT NULL,
    tags             TEXT[],
    operation_rating NUMERIC(2,1),
    comment          TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_companion_reviews_companion ON companion_reviews(companion_id);
CREATE INDEX IF NOT EXISTS idx_companion_reviews_reviewee ON companion_reviews(reviewee_id);
CREATE INDEX IF NOT EXISTS idx_companion_reviews_reviewer ON companion_reviews(reviewer_id);
