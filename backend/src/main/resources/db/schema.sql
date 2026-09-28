-- FitPulse schema. Runs on MySQL 8 and, in MySQL mode, on H2 (used by the automated tests).
-- Every statement is CREATE TABLE IF NOT EXISTS, so running it again is harmless.
-- `seq` keeps the order rows were added, so lists come back in the same order as in memory.

CREATE TABLE IF NOT EXISTS users (
    id      VARCHAR(40)  NOT NULL PRIMARY KEY,
    name    VARCHAR(120) NOT NULL,
    role    VARCHAR(20)  NOT NULL,
    active  BOOLEAN      NOT NULL,
    seq     BIGINT       NOT NULL AUTO_INCREMENT UNIQUE
);

-- Only a salted PBKDF2 hash is stored, never the password.
CREATE TABLE IF NOT EXISTS credentials (
    user_id        VARCHAR(40)  NOT NULL PRIMARY KEY,
    password_hash  VARCHAR(255) NOT NULL,
    CONSTRAINT fk_credentials_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS equipment (
    id                       VARCHAR(40)  NOT NULL PRIMARY KEY,
    name                     VARCHAR(120) NOT NULL,
    type                     VARCHAR(30)  NOT NULL,
    location                 VARCHAR(80)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    active                   BOOLEAN      NOT NULL,
    total_usage_hours        DOUBLE       NOT NULL,
    hours_since_maintenance  DOUBLE       NOT NULL,
    threshold_hours          DOUBLE       NOT NULL,
    seq                      BIGINT       NOT NULL AUTO_INCREMENT UNIQUE
);

CREATE TABLE IF NOT EXISTS maintenance_requests (
    id            VARCHAR(40)  NOT NULL PRIMARY KEY,
    equipment_id  VARCHAR(40)  NOT NULL,
    description   VARCHAR(500) NOT NULL,
    urgency       VARCHAR(10)  NOT NULL,
    status        VARCHAR(12)  NOT NULL,
    reported_by   VARCHAR(40)  NOT NULL,
    assigned_to   VARCHAR(120) NULL,
    seq           BIGINT       NOT NULL AUTO_INCREMENT UNIQUE,
    CONSTRAINT fk_requests_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT fk_requests_reporter  FOREIGN KEY (reported_by)  REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS maintenance_notes (
    seq         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id  VARCHAR(40)  NOT NULL,
    note        VARCHAR(500) NOT NULL,
    CONSTRAINT fk_notes_request FOREIGN KEY (request_id) REFERENCES maintenance_requests (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sessions (
    id             VARCHAR(40)  NOT NULL PRIMARY KEY,
    title          VARCHAR(120) NOT NULL,
    instructor_id  VARCHAR(40)  NOT NULL,
    studio         VARCHAR(80)  NOT NULL,
    start_at       DATETIME     NOT NULL,
    end_at         DATETIME     NOT NULL,
    capacity       INT          NOT NULL,
    seq            BIGINT       NOT NULL AUTO_INCREMENT UNIQUE,
    CONSTRAINT fk_sessions_instructor FOREIGN KEY (instructor_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS session_equipment (
    session_id    VARCHAR(40) NOT NULL,
    equipment_id  VARCHAR(40) NOT NULL,
    sort_order    INT         NOT NULL,
    PRIMARY KEY (session_id, equipment_id),
    CONSTRAINT fk_sessioneq_session   FOREIGN KEY (session_id)   REFERENCES sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_sessioneq_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id)
);

CREATE TABLE IF NOT EXISTS bookings (
    seq         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(40) NOT NULL,
    user_id     VARCHAR(40) NOT NULL,
    CONSTRAINT uq_booking UNIQUE (session_id, user_id),
    CONSTRAINT fk_bookings_session FOREIGN KEY (session_id) REFERENCES sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_user    FOREIGN KEY (user_id)    REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(40)   NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activity_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entry       VARCHAR(1000) NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
