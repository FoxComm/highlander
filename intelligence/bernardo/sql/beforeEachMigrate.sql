DO $$
DECLARE
    default_search_path TEXT := NULL;
BEGIN
    SELECT setting_value INTO default_search_path
    FROM (
        SELECT
            substring(setting from 1 for eq_position - 1) AS setting_name,
            substring(setting from eq_position + 1) AS setting_value
        FROM (
            SELECT position('=' in setting) AS eq_position, setting
            FROM (
                SELECT UNNEST(setconfig) AS setting FROM pg_db_role_setting s JOIN pg_database d ON s.setdatabase = d.oid WHERE d.datname = current_database()
            ) raw_db_role_settings
        ) presplit_db_role_settings
    ) split_db_role_settings
    WHERE setting_name = 'search_path';

    IF default_search_path IS NOT NULL THEN
        EXECUTE 'SET search_path = '||default_search_path;
    END IF;
END;
$$