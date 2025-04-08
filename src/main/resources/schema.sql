CREATE TABLE IF NOT EXISTS public.note (
    id int8 NOT NULL,
    "version" int8 NOT NULL,
    "content" varchar(255) NULL,
    creation_date timestamp(6) NULL,
    expiration_date timestamp(6) NULL,
    title varchar(120) NULL,
    CONSTRAINT note_pkey PRIMARY KEY (id, version)
);
CREATE INDEX IF NOT EXISTS idx_note_creation_date ON public.note USING btree (creation_date);
CREATE INDEX IF NOT EXISTS idx_note_expiration_date ON public.note USING btree (expiration_date);

CREATE TABLE IF NOT EXISTS public.notes_user (
    id uuid NOT NULL,
    username varchar(255) NULL,
    "password" varchar(255) NULL,
    CONSTRAINT notes_user_pkey PRIMARY KEY (id),
    CONSTRAINT notes_user_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_user_username ON public.notes_user USING btree (username);

CREATE TABLE IF NOT EXISTS public.user_note (
    note_id int8 NOT NULL,
    user_id uuid NOT NULL,
    active_version int8 NOT NULL,
    deleted bool NOT NULL,
    CONSTRAINT user_note_pkey PRIMARY KEY (note_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_usernote_deleted ON public.user_note USING btree (deleted);
CREATE INDEX IF NOT EXISTS idx_usernote_version ON public.user_note USING btree (active_version);


CREATE SEQUENCE IF NOT EXISTS note_seq INCREMENT BY 1 MINVALUE 1;
