-- One-time MySQL setup for FitPulse. Run it as a MySQL administrator (for WAMP: phpMyAdmin > SQL tab,
-- or:  mysql -u root -p < backend/db/init.sql ).
--
-- 1. Replace CHANGE_ME below with a password of your own choice (do not commit the edited file).
-- 2. Use the same password in the FITPULSE_DB_PASSWORD environment variable when you start the app.
--
-- The application creates every table itself on start (backend/src/main/resources/db/schema.sql),
-- so only the database and the login are created here.

CREATE DATABASE IF NOT EXISTS fitpulse CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'fitpulse'@'localhost' IDENTIFIED BY 'CHANGE_ME';

GRANT ALL PRIVILEGES ON fitpulse.* TO 'fitpulse'@'localhost';

FLUSH PRIVILEGES;
