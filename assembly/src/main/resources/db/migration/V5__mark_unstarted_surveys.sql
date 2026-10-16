UPDATE survey s
SET status = 'NOT_STARTED'
WHERE s.status = 'CLOSED'
  AND NOT EXISTS (
      SELECT 1
      FROM voting_session vs
      WHERE vs.survey_id = s.id
  );
