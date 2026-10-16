CREATE TABLE IF NOT EXISTS voting_result_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(100) NOT NULL,
    survey_id BIGINT NOT NULL REFERENCES survey (id),
    routing_key VARCHAR(32) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_voting_result_outbox_event_key
    ON voting_result_outbox (event_key);

CREATE INDEX IF NOT EXISTS idx_voting_result_outbox_pending
    ON voting_result_outbox (status, created_at);
