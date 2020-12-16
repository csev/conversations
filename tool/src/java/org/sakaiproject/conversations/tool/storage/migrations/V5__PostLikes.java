package org.sakaiproject.conversations.tool.storage.migrations;

import org.sakaiproject.conversations.tool.storage.DBConnection;

public class V5__PostLikes extends BaseMigration {

    final static String TABLE_DEFS =
            "CREATE TABLE conversations_post_like ( " +
            "    post_uuid varchar2(255) NOT NULL,   " +
            "    user_id varchar2(255) NOT NULL,     " +
            "    CONSTRAINT pk_conv_post_like PRIMARY KEY(post_uuid, user_id) "+
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
