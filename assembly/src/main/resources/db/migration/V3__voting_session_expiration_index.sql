CREATE INDEX IF NOT EXISTS idx_voting_session_status_end_time
    ON voting_session (status, end_time);
