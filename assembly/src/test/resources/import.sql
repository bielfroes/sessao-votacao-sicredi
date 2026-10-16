CREATE UNIQUE INDEX uk_elector_cpf ON elector (cpf);
CREATE UNIQUE INDEX uk_vote_survey_elector ON vote (survey_id, elector_id);
CREATE INDEX idx_vote_elector_id ON vote (elector_id);
CREATE INDEX idx_survey_status_end_time ON survey (status, end_time);
CREATE INDEX idx_voting_session_survey_id ON voting_session (survey_id);
CREATE INDEX idx_voting_session_status_end_time ON voting_session (status, end_time);
CREATE INDEX idx_voting_result_outbox_pending ON voting_result_outbox (status, created_at);
