-- Выполнить один раз после перехода на 6 полей цен (если в таблице equipment ещё есть старые колонки).
-- PostgreSQL:
ALTER TABLE equipment DROP COLUMN IF EXISTS price_per_hour;
ALTER TABLE equipment DROP COLUMN IF EXISTS price_per_day;
ALTER TABLE equipment DROP COLUMN IF EXISTS price_per_week;
ALTER TABLE equipment DROP COLUMN IF EXISTS price_per_month;
