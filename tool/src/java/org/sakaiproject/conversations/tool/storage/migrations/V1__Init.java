package org.sakaiproject.conversations.tool.storage.migrations;

import org.sakaiproject.conversations.tool.storage.DB;
import org.sakaiproject.conversations.tool.storage.DBConnection;

public class V1__Init extends BaseMigration {

    final static String TABLE_DEFS =
        "CREATE TABLE conversations_topic (       " +
        "    uuid varchar2(255) PRIMARY KEY,      " +
        "    site_id varchar2(255) NOT NULL,      " +
        "    title varchar2(255) NOT NULL,        " +
        "    type varchar2(255) NOT NULL,         " +
        "    created_by varchar2(255) NOT NULL,   " +
        "    created_at NUMBER NOT NULL,          " +
        "    last_activity_at NUMBER NOT NULL     " +
        ");                                       " +

        "CREATE TABLE conversations_post (        " +
        "    uuid varchar2(255) PRIMARY KEY,      " +
        "    topic_uuid varchar2(255) NOT NULL,   " +
        "    parent_post_uuid varchar2(255),      " +
        "    content CLOB NOT NULL,               " +
        "    posted_by varchar2(255) NOT NULL,    " +
        "    posted_at NUMBER NOT NULL,           " +
        "    updated_by varchar2(255) NOT NULL,   " +
        "    updated_at NUMBER NOT NULL,          " +
        "    version NUMBER NOT NULL              " +
        ");                                       " +

        "CREATE TABLE conversations_topic_event ( " +
        "    uuid varchar2(255) PRIMARY KEY,      " +
        "    topic_uuid varchar2(255) NOT NULL,   " +
        "    user_id varchar2(255) NOT NULL,      " +
        "    event_name varchar2(255) NOT NULL,   " +
        "    event_time NUMBER NOT NULL           " +
        ");                                       " +

        "CREATE TABLE conversations_files (       " +
        "    uuid varchar2(255) PRIMARY KEY,      " +
        "    mime_type varchar2(255) NOT NULL,    " +
        "    filename varchar2(255) NOT NULL,     " +
        "    role varchar2(32) NOT NULL           " +
        ");                                       " +

        "CREATE TABLE conversations_attachments ( " +
        "    uuid varchar2(255) PRIMARY KEY,      " +
        "    post_uuid varchar2(255),             " +
        "    topic_uuid varchar2(255),            " +
        "    attachment_key varchar2(255) NOT NULL" +
        ");                                       ";



    public void migrate(DBConnection connection) throws Exception {
        for (String ddl : TABLE_DEFS.split(";")) {
            if (ddl.trim().isEmpty()) {
                continue;
            }

            connection.run(ddl.trim()).executeUpdate();
        }
    }
}
