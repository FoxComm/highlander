-- use libpq-style connection info string in dblink function, for example hostaddr=127.0.0.1 port=5432 dbname=mydb user=postgres password=mypasswd
CREATE EXTENSION IF NOT EXISTS dblink;
INSERT INTO sku_mwh_sku_ids (sku_id, mwh_sku_id) SELECT skus.id, mwh_skus.id FROM dblink('dbname=middlewarehouse', 'SELECT id, code FROM skus') AS mwh_skus(id int, code text) INNER JOIN skus ON mwh_skus.code = skus.code ON CONFLICT (sku_id) DO UPDATE SET mwh_sku_id=EXCLUDED.mwh_sku_id;
DROP EXTENSION IF EXISTS dblink;


