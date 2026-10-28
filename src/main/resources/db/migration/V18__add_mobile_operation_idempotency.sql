ALTER TABLE workshop.post_comment
    ADD COLUMN client_operation_id UUID;

CREATE UNIQUE INDEX post_comment_user_operation_unique
    ON workshop.post_comment (user_id, client_operation_id)
    WHERE client_operation_id IS NOT NULL;

ALTER TABLE workshop.chat_message
    ADD COLUMN client_operation_id UUID;

CREATE UNIQUE INDEX chat_message_author_operation_unique
    ON workshop.chat_message (author_id, client_operation_id)
    WHERE client_operation_id IS NOT NULL;

ALTER TABLE workshop.evaluation
    ADD COLUMN client_operation_id UUID;

CREATE UNIQUE INDEX evaluation_user_operation_unique
    ON workshop.evaluation (user_id, client_operation_id)
    WHERE client_operation_id IS NOT NULL;
