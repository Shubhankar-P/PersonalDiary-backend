-- V2__seed_config.sql
-- Seed the quotes_api config value that was present in the old MongoDB
-- config collection but was never carried over during the schema migration.
INSERT INTO config_diary_app (config_key, config_value)
VALUES ('quotes_api', 'http://api.forismatic.com/api/1.0/?method=getQuote&format=json&lang=en');