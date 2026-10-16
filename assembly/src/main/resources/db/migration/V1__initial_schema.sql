CREATE TABLE IF NOT EXISTS survey (
    id BIGSERIAL PRIMARY KEY,
    end_time TIMESTAMP,
    question VARCHAR(500),
    status VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS elector (
    id BIGSERIAL PRIMARY KEY,
    cpf VARCHAR(32),
    status VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS vote (
    id BIGSERIAL PRIMARY KEY,
    survey_id BIGINT,
    elector_id BIGINT,
    approval BOOLEAN
);
