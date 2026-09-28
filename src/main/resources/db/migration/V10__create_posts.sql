CREATE TABLE workshop.post (
 id UUID PRIMARY KEY, title VARCHAR(180) NOT NULL, content TEXT NOT NULL, image VARCHAR(500), workshop_id UUID REFERENCES workshop.workshop(id), category_id UUID REFERENCES workshop.category(id), status VARCHAR(20) NOT NULL, highlight BOOLEAN NOT NULL DEFAULT FALSE, scheduled_at TIMESTAMPTZ, published_at TIMESTAMPTZ, created_by UUID NOT NULL REFERENCES workshop.app_user(id), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CHECK (status IN ('DRAFT','SCHEDULED','PUBLISHED','ARCHIVED'))
);
CREATE TABLE workshop.post_like (post_id UUID NOT NULL REFERENCES workshop.post(id) ON DELETE CASCADE, user_id UUID NOT NULL REFERENCES workshop.app_user(id), created_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(post_id,user_id));
CREATE TABLE workshop.post_comment (id UUID PRIMARY KEY, post_id UUID NOT NULL REFERENCES workshop.post(id) ON DELETE CASCADE, user_id UUID NOT NULL REFERENCES workshop.app_user(id), content TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL);
CREATE INDEX post_published_feed ON workshop.post(status, highlight DESC, published_at DESC);
CREATE INDEX post_comment_page ON workshop.post_comment(post_id, created_at DESC);
