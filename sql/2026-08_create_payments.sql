-- Phase 7: 수익 모델 · 결제 연동 (Mock PG)
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 반드시 수동으로 실행할 것.
-- 실행 순서: 2026-08_create_guide_applications.sql 이후에 실행

-- 1. companions 테이블에 모집글 부스트 만료 시각 컬럼 추가
ALTER TABLE companions ADD COLUMN IF NOT EXISTS boosted_until TIMESTAMP;

-- 2. 결제 내역 테이블 (참여 수수료 / 보증금 / 모집글 부스트 공용)
-- UUID 기본값은 DB가 아닌 Hibernate(GenerationType.UUID)가 애플리케이션에서 채워 넣으므로
-- DEFAULT gen_random_uuid()를 걸지 않는다.
CREATE TABLE IF NOT EXISTS payments (
    payment_id      UUID PRIMARY KEY,
    payer_id        UUID NOT NULL REFERENCES users(user_id),
    companion_id    UUID NOT NULL REFERENCES companions(companion_id),
    type            VARCHAR(30) NOT NULL,   -- PARTICIPATION_FEE / DEPOSIT / BOOST
    amount          NUMERIC(10, 0) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING / PAID / REFUNDED / FORFEITED / FAILED
    transaction_key VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    paid_at         TIMESTAMP,
    resolved_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payments_payer ON payments(payer_id);
CREATE INDEX IF NOT EXISTS idx_payments_companion ON payments(companion_id);
CREATE INDEX IF NOT EXISTS idx_payments_companion_type_status ON payments(companion_id, type, status);
