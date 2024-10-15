ALTER TABLE Access
ADD COLUMN userDomain VARCHAR(255) DEFAULT null;

-- Dropping the PRIMARY KEY and adding a UNIQUE constraint with both Id and Domain,
-- because Postgresql doesn't like a nullable fields being PRIMARY KEY.
-- (previous tentative: PRIMARY KEY (userId, userDomain)
ALTER TABLE Access
DROP CONSTRAINT IF EXISTS access_pkey;

ALTER TABLE Access ADD CONSTRAINT access_user_id_user_domain_key UNIQUE (userId, userDomain);

ALTER TABLE Events
ADD COLUMN userDomain VARCHAR(255) DEFAULT null;

ALTER TABLE Events
ADD COLUMN conversationDomain VARCHAR(255) DEFAULT null;

DROP INDEX conversation_id_idx;
CREATE INDEX events_conversation_id_conversation_domain_idx ON Events (conversationId, conversationDomain);
