CREATE TYPE "job_status" AS ENUM (
  'PENDING',
  'RUNNING',
  'COMPLETED',
  'FAILED'
);

CREATE TYPE "image_status" AS ENUM (
  'PENDING',
  'RUNNING',
  'UPLOADED',
  'INVALID',
  'EXPIRED'
);

CREATE TABLE "user"
(
    "id"                   UUID PRIMARY KEY,
    "username"             VARCHAR UNIQUE NOT NULL,
    "email"                VARCHAR UNIQUE,
    "phone_number"         VARCHAR UNIQUE,
    "email_confirm"        BOOL           NOT NULL,
    "phone_number_confirm" BOOL           NOT NULL,
    "password_hash"        VARCHAR        NOT NULL,
    "created_at"           TIMESTAMP      NOT NULL,
    "updated_at"           TIMESTAMP
);

CREATE TABLE "refresh_token"
(
    "id"            UUID PRIMARY KEY,
    "user_id"       UUID           NOT NULL,
    "refresh_token" VARCHAR UNIQUE NOT NULL,
    "access_token"  VARCHAR        NOT NULL,
    "ttl_sec"       INTEGER        NOT NULL,
    "created_at"    TIMESTAMP      NOT NULL,
    "expired_at"    TIMESTAMP      NOT NULL
);

CREATE TABLE "user_image"
(
    "id"                UUID PRIMARY KEY,
    "user_id"           UUID         NOT NULL,
    "uploaded_image_id" UUID,
    "status"            IMAGE_STATUS NOT NULL,
    "file_name"         VARCHAR      NOT NULL,
    "created_at"        TIMESTAMP    NOT NULL,
    "updated_at"        TIMESTAMP,
    "deleted_at"        TIMESTAMP,
    "expired_at"        TIMESTAMP
);

CREATE TABLE "uploaded_image"
(
    "id"         UUID PRIMARY KEY,
    "width"      INTEGER        NOT NULL,
    "height"     INTEGER        NOT NULL,
    "file_size"  INTEGER        NOT NULL,
    "file_path"  VARCHAR        NOT NULL,
    "file_type"  VARCHAR        NOT NULL,
    "file_hash"  VARCHAR UNIQUE NOT NULL,
    "status"     IMAGE_STATUS   NOT NULL,
    "created_at" TIMESTAMP      NOT NULL,
    "updated_at" TIMESTAMP
);

CREATE TABLE "generated_image"
(
    "id"               UUID PRIMARY KEY,
    "image_id"         UUID           NOT NULL,
    "configuration_id" UUID           NOT NULL,
    "width"            INTEGER        NOT NULL,
    "height"           INTEGER        NOT NULL,
    "file_size"        INTEGER        NOT NULL,
    "file_path"        VARCHAR        NOT NULL,
    "file_type"        VARCHAR        NOT NULL,
    "file_hash"        VARCHAR UNIQUE NOT NULL,
    "created_at"       TIMESTAMP      NOT NULL
);

CREATE TABLE "processing_job_configuration"
(
    "id"               UUID PRIMARY KEY,
    "width"            INTEGER,
    "height"           INTEGER,
    "scale"            DECIMAL,
    "keep_ratio"       BOOLEAN,
    "quality"          INTEGER,
    "description"      VARCHAR,
    "output_file_type" VARCHAR,
    "file_type"        VARCHAR
);

CREATE TABLE "processing_job"
(
    "id"               UUID PRIMARY KEY,
    "image_id"         UUID,
    "configuration_id" UUID,
    "job_status"       JOB_STATUS NOT NULL,
    "started_at"       TIMESTAMP  NOT NULL,
    "ended_at"         TIMESTAMP,
    "remark"           VARCHAR
);

CREATE UNIQUE INDEX ON "generated_image" ("image_id", "configuration_id");

CREATE UNIQUE INDEX ON "processing_job" ("image_id", "configuration_id");

ALTER TABLE "refresh_token"
    ADD FOREIGN KEY ("user_id") REFERENCES "user" ("id");

ALTER TABLE "user_image"
    ADD FOREIGN KEY ("user_id") REFERENCES "user" ("id");

ALTER TABLE "user_image"
    ADD FOREIGN KEY ("uploaded_image_id") REFERENCES "uploaded_image" ("id");

ALTER TABLE "generated_image"
    ADD FOREIGN KEY ("image_id") REFERENCES "uploaded_image" ("id");

ALTER TABLE "generated_image"
    ADD FOREIGN KEY ("configuration_id") REFERENCES "processing_job_configuration" ("id");

ALTER TABLE "processing_job"
    ADD FOREIGN KEY ("image_id") REFERENCES "uploaded_image" ("id");

ALTER TABLE "processing_job"
    ADD FOREIGN KEY ("configuration_id") REFERENCES "processing_job_configuration" ("id");
