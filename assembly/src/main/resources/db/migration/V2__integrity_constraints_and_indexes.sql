ALTER TABLE survey
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;

UPDATE survey
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE survey
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE survey
    ALTER COLUMN question TYPE VARCHAR(500);

CREATE TABLE IF NOT EXISTS voting_session (
    id BIGSERIAL PRIMARY KEY,
    survey_id BIGINT NOT NULL REFERENCES survey (id),
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO voting_session (survey_id, opened_at, end_time, status)
SELECT s.id,
       COALESCE(s.created_at, CURRENT_TIMESTAMP),
       s.end_time AT TIME ZONE 'America/Sao_Paulo',
       CASE
           WHEN s.status = 'OPEN'
                AND s.end_time > (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo') THEN 'OPEN'
           ELSE 'CLOSED'
       END
FROM survey s
WHERE s.end_time IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM voting_session vs WHERE vs.survey_id = s.id
  );

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM vote
        GROUP BY survey_id, elector_id
        HAVING survey_id IS NULL OR elector_id IS NULL OR COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add vote uniqueness: resolve duplicate or null survey_id/elector_id rows first';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM vote v
        LEFT JOIN survey s ON s.id = v.survey_id
        WHERE s.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot add vote foreign key: resolve orphan survey references first';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM vote v
        LEFT JOIN elector e ON e.id = v.elector_id
        WHERE e.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot add vote foreign key: resolve orphan elector references first';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM elector
        GROUP BY cpf
        HAVING cpf IS NULL OR COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add CPF uniqueness: resolve duplicate or null CPF rows first';
    END IF;
END $$;

ALTER TABLE vote
    ALTER COLUMN survey_id SET NOT NULL,
    ALTER COLUMN elector_id SET NOT NULL,
    ALTER COLUMN approval SET NOT NULL;

ALTER TABLE elector
    ALTER COLUMN cpf TYPE VARCHAR(11),
    ALTER COLUMN cpf SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vote_survey_elector
    ON vote (survey_id, elector_id);
CREATE INDEX IF NOT EXISTS idx_vote_elector_id
    ON vote (elector_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_elector_cpf
    ON elector (cpf);
CREATE INDEX IF NOT EXISTS idx_survey_status_end_time
    ON survey (status, end_time);
CREATE INDEX IF NOT EXISTS idx_voting_session_survey_id
    ON voting_session (survey_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_voting_session_one_open_per_survey
    ON voting_session (survey_id)
    WHERE status = 'OPEN';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_vote_survey'
    ) THEN
        ALTER TABLE vote
            ADD CONSTRAINT fk_vote_survey
            FOREIGN KEY (survey_id) REFERENCES survey (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_vote_elector'
    ) THEN
        ALTER TABLE vote
            ADD CONSTRAINT fk_vote_elector
            FOREIGN KEY (elector_id) REFERENCES elector (id);
    END IF;
END $$;
