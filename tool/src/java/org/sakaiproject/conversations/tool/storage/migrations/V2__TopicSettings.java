package org.sakaiproject.conversations.tool.storage.migrations;

import org.sakaiproject.conversations.tool.storage.DBConnection;

public class V2__TopicSettings extends BaseMigration {

    final static String TABLE_DEFS =
        "CREATE TABLE conversations_topic_settings ( " +
        "    topic_uuid varchar2(255) PRIMARY KEY,   " +
        "    availability varchar2(255) NOT NULL,    " +
        "    published NUMBER(1,0) NOT NULL,         " +
        "    graded NUMBER(1,0) NOT NULL,            " +
        "    allow_comments NUMBER(1,0) NOT NULL,    " +
        "    allow_like NUMBER(1,0) NOT NULL,        " +
        "    require_post NUMBER(1,0) NOT NULL       " +
        ");                                          ";



    public void migrate(DBConnection connection) throws Exception {
        for (String ddl : TABLE_DEFS.split(";")) {
            if (ddl.trim().isEmpty()) {
                continue;
            }

            connection.run(ddl.trim()).executeUpdate();
        }
    }
}
