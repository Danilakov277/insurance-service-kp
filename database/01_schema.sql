-- =====================================================================
-- ПС «Страховой сервис»: скрипт генерации базы данных (PostgreSQL 16)
-- Создание: psql -U postgres -f 01_schema.sql
-- =====================================================================
DROP DATABASE IF EXISTS insurance WITH (FORCE);
CREATE DATABASE insurance ENCODING 'UTF8' TEMPLATE template0;
\connect insurance

-- Роли пользователей
CREATE TABLE roles (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- Учётные записи
CREATE TABLE users (
    id            SERIAL PRIMARY KEY,
    login         VARCHAR(50)  NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,
    salt          CHAR(32)     NOT NULL,
    role_id       INTEGER      NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    is_blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_login CHECK (length(login) >= 3)
);

-- Клиенты (страхователи)
CREATE TABLE clients (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER      NOT NULL UNIQUE REFERENCES users (id) ON DELETE RESTRICT,
    last_name   VARCHAR(50)  NOT NULL,
    first_name  VARCHAR(50)  NOT NULL,
    patronymic  VARCHAR(50),
    birth_date  DATE         NOT NULL,
    passport_no VARCHAR(20)  NOT NULL UNIQUE,
    phone       VARCHAR(20)  NOT NULL,
    address     VARCHAR(200) NOT NULL,
    CONSTRAINT chk_clients_birth CHECK (birth_date > DATE '1900-01-01')
);

-- Страховые агенты
CREATE TABLE agents (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER      NOT NULL UNIQUE REFERENCES users (id) ON DELETE RESTRICT,
    last_name       VARCHAR(50)  NOT NULL,
    first_name      VARCHAR(50)  NOT NULL,
    patronymic      VARCHAR(50),
    position        VARCHAR(60)  NOT NULL,
    hire_date       DATE         NOT NULL,
    commission_rate NUMERIC(5,4) NOT NULL DEFAULT 0.1000,
    CONSTRAINT chk_agents_commission CHECK (commission_rate >= 0 AND commission_rate <= 0.5)
);

-- Виды страхования и тарифы
CREATE TABLE insurance_types (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(80)   NOT NULL UNIQUE,
    category        VARCHAR(20)   NOT NULL,
    description     TEXT,
    base_rate       NUMERIC(6,4)  NOT NULL,
    min_sum         NUMERIC(12,2) NOT NULL,
    max_sum         NUMERIC(12,2) NOT NULL,
    max_term_months INTEGER       NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_types_category CHECK (category IN ('PROPERTY', 'CASCO', 'ACCIDENT', 'TRAVEL')),
    CONSTRAINT chk_types_rate CHECK (base_rate > 0 AND base_rate < 1),
    CONSTRAINT chk_types_sums CHECK (min_sum > 0 AND max_sum >= min_sum),
    CONSTRAINT chk_types_term CHECK (max_term_months BETWEEN 1 AND 60)
);

-- Заявки на страхование
CREATE TABLE applications (
    id                 SERIAL PRIMARY KEY,
    client_id          INTEGER       NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    type_id            INTEGER       NOT NULL REFERENCES insurance_types (id) ON DELETE RESTRICT,
    agent_id           INTEGER       REFERENCES agents (id) ON DELETE RESTRICT,
    requested_sum      NUMERIC(12,2) NOT NULL,
    term_months        INTEGER       NOT NULL,
    franchise_percent  INTEGER       NOT NULL DEFAULT 0,
    installments       INTEGER       NOT NULL DEFAULT 1,
    object_description TEXT          NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    decision_comment   TEXT,
    created_at         TIMESTAMP     NOT NULL DEFAULT now(),
    decided_at         TIMESTAMP,
    CONSTRAINT chk_app_status CHECK (status IN ('NEW', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_app_sum CHECK (requested_sum > 0),
    CONSTRAINT chk_app_term CHECK (term_months > 0),
    CONSTRAINT chk_app_franchise CHECK (franchise_percent IN (0, 1, 2, 5)),
    CONSTRAINT chk_app_installments CHECK (installments IN (1, 2, 4, 12))
);

-- Договоры страхования (полисы)
CREATE TABLE policies (
    id             SERIAL PRIMARY KEY,
    policy_number  VARCHAR(20)   NOT NULL UNIQUE,
    application_id INTEGER       UNIQUE REFERENCES applications (id) ON DELETE RESTRICT,
    client_id      INTEGER       NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    agent_id       INTEGER       NOT NULL REFERENCES agents (id) ON DELETE RESTRICT,
    type_id        INTEGER       NOT NULL REFERENCES insurance_types (id) ON DELETE RESTRICT,
    insured_sum    NUMERIC(12,2) NOT NULL,
    premium        NUMERIC(12,2) NOT NULL,
    franchise      NUMERIC(12,2) NOT NULL DEFAULT 0,
    start_date     DATE          NOT NULL,
    end_date       DATE          NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'WAITING_PAYMENT',
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    terminated_at  TIMESTAMP,
    refund_amount  NUMERIC(12,2),
    CONSTRAINT chk_pol_status CHECK (status IN
        ('WAITING_PAYMENT', 'ACTIVE', 'SUSPENDED', 'EXPIRED', 'TERMINATED', 'FULFILLED')),
    CONSTRAINT chk_pol_sums CHECK (insured_sum > 0 AND premium > 0 AND franchise >= 0),
    CONSTRAINT chk_pol_dates CHECK (end_date > start_date)
);

-- Страховые взносы (зависимая сущность, идентифицирующая связь с договором)
CREATE TABLE payments (
    policy_id      INTEGER       NOT NULL REFERENCES policies (id) ON DELETE CASCADE,
    installment_no INTEGER       NOT NULL,
    amount         NUMERIC(12,2) NOT NULL,
    due_date       DATE          NOT NULL,
    paid_date      DATE,
    PRIMARY KEY (policy_id, installment_no),
    CONSTRAINT chk_pay_amount CHECK (amount > 0),
    CONSTRAINT chk_pay_no CHECK (installment_no > 0)
);

-- Страховые случаи
CREATE TABLE claims (
    id               SERIAL PRIMARY KEY,
    policy_id        INTEGER       NOT NULL REFERENCES policies (id) ON DELETE RESTRICT,
    incident_date    DATE          NOT NULL,
    report_date      DATE          NOT NULL,
    description      TEXT          NOT NULL,
    damage_amount    NUMERIC(12,2) NOT NULL,
    assessed_damage  NUMERIC(12,2),
    status           VARCHAR(20)   NOT NULL DEFAULT 'REPORTED',
    payout_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    agent_id         INTEGER       REFERENCES agents (id) ON DELETE RESTRICT,
    decision_comment TEXT,
    decided_at       TIMESTAMP,
    CONSTRAINT chk_claim_status CHECK (status IN ('REPORTED', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_claim_amounts CHECK (damage_amount > 0 AND payout_amount >= 0),
    CONSTRAINT chk_claim_dates CHECK (report_date >= incident_date)
);

-- Индексы для частых выборок
CREATE INDEX idx_app_status   ON applications (status);
CREATE INDEX idx_pol_client   ON policies (client_id);
CREATE INDEX idx_pol_status   ON policies (status);
CREATE INDEX idx_pay_due      ON payments (due_date) WHERE paid_date IS NULL;
CREATE INDEX idx_claim_policy ON claims (policy_id);

INSERT INTO roles (name) VALUES ('CLIENT'), ('AGENT'), ('ADMIN');
