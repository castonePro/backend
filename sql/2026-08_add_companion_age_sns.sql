-- 동행 탐색 카드/필터 보강: 모집 나이대 + 방장 SNS/커뮤니티 아이디
-- ddl-auto: validate 설정이라 Hibernate가 스키마를 자동 생성하지 않는다.
-- 로컬 DB와 RDS(Aurora) 양쪽에 반드시 수동으로 실행할 것.
-- 실행 순서: 2026-08_create_payments.sql 이후에 실행

ALTER TABLE companions ADD COLUMN IF NOT EXISTS min_age INT;
ALTER TABLE companions ADD COLUMN IF NOT EXISTS max_age INT;
ALTER TABLE companions ADD COLUMN IF NOT EXISTS sns_handle VARCHAR(50);
