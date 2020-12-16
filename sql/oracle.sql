CREATE TABLE conversations_topic (
    uuid varchar2(255) PRIMARY KEY,
    site_id varchar2(255) NOT NULL,
    title varchar2(255) NOT NULL,
    type varchar2(255) NOT NULL,
    created_by varchar2(255) NOT NULL,
    created_at NUMBER NOT NULL,
    last_activity_at NUMBER NOT NULL
);


CREATE TABLE conversations_post (
    uuid varchar2(255) PRIMARY KEY,
    topic_uuid varchar2(255) NOT NULL,
    parent_post_uuid varchar2(255),
    content CLOB NOT NULL,
    posted_by varchar2(255) NOT NULL,
    posted_at NUMBER NOT NULL,
    updated_by varchar2(255) NOT NULL,
    updated_at NUMBER NOT NULL,
    version NUMBER NOT NULL
);

CREATE TABLE conversations_topic_event (
    uuid varchar2(255) PRIMARY KEY,
    topic_uuid varchar2(255) NOT NULL,
    user_id varchar2(255) NOT NULL,
    event_name varchar2(255) NOT NULL,
    event_time NUMBER NOT NULL
);

CREATE TABLE conversations_files (
    uuid varchar2(255) PRIMARY KEY,
    mime_type varchar2(255) NOT NULL,
    filename varchar2(255) NOT NULL,
    role varchar2(32) NOT NULL
);

CREATE TABLE conversations_attachments (
    uuid varchar2(255) PRIMARY KEY,
    post_uuid varchar2(255),
    topic_uuid varchar2(255),
    attachment_key varchar2(255) NOT NULL
);

CREATE TABLE conversations_topic_settings (
    topic_uuid varchar2(255) PRIMARY KEY,
    availability varchar2(255) NOT NULL,
    published NUMBER(1,0) NOT NULL,
    graded NUMBER(1,0) NOT NULL,
    allow_comments NUMBER(1,0) NOT NULL,
    allow_like NUMBER(1,0) NOT NULL,
    require_post NUMBER(1,0) NOT NULL
);

CREATE TABLE conversations_topic_group (
    topic_uuid varchar2(255) NOT NULL,
    group_id varchar2(255) NOT NULL,
    CONSTRAINT pk_conv_topic_group PRIMARY KEY(topic_uuid, group_id)
);

CREATE TABLE conversations_post_like (
    post_uuid varchar2(255) NOT NULL,
    user_id varchar2(255) NOT NULL,
    CONSTRAINT pk_conv_post_like PRIMARY KEY(post_uuid, user_id)
);


CREATE INDEX conv_topic_site_id ON conversations_topic (site_id);
CREATE INDEX conv_topic_title ON conversations_topic (title);
CREATE INDEX conv_topic_type ON conversations_topic (type);
CREATE INDEX conv_topic_last_activity_at ON conversations_topic (last_activity_at);
CREATE INDEX conv_post_topic_uuid ON conversations_post (topic_uuid);
CREATE INDEX conv_post_posted_by ON conversations_post (posted_by);
CREATE INDEX conv_post_posted_at ON conversations_post (posted_at);
CREATE INDEX conv_topic_event_t_u_e_e ON conversations_topic_event (topic_uuid, user_id, event_name, event_time);
CREATE INDEX conv_attachments_att_key ON conversations_attachments (attachment_key);
CREATE INDEX conv_attachments_post_uuid ON conversations_attachments (post_uuid);
CREATE INDEX conv_topic_settings_avail ON conversations_topic_settings (availability);
CREATE INDEX conv_topic_settings_published ON conversations_topic_settings (published);
CREATE INDEX conv_topic_group ON conversations_topic_group (group_id);
