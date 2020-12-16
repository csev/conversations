package org.sakaiproject.conversations.tool.storage.migrations;

import org.sakaiproject.conversations.tool.storage.DBConnection;

public class V6__Indexes extends BaseMigration {

    final static String INDEXES = (
                                   "CREATE INDEX conv_topic_site_id ON conversations_topic (site_id);" +
                                   "CREATE INDEX conv_topic_title ON conversations_topic (title);" +
                                   "CREATE INDEX conv_topic_type ON conversations_topic (type);" +
                                   "CREATE INDEX conv_topic_last_activity_at ON conversations_topic (last_activity_at);" +
                                   "CREATE INDEX conv_post_topic_uuid ON conversations_post (topic_uuid);" +
                                   "CREATE INDEX conv_post_posted_by ON conversations_post (posted_by);" +
                                   "CREATE INDEX conv_post_posted_at ON conversations_post (posted_at);" +
                                   "CREATE INDEX conv_topic_event_t_u_e_e ON conversations_topic_event (topic_uuid, user_id, event_name, event_time);" +
                                   "CREATE INDEX conv_attachments_att_key ON conversations_attachments (attachment_key);" +
                                   "CREATE INDEX conv_attachments_post_uuid ON conversations_attachments (post_uuid);" +
                                   "CREATE INDEX conv_topic_settings_avail ON conversations_topic_settings (availability);" +
                                   "CREATE INDEX conv_topic_settings_published ON conversations_topic_settings (published);" +
                                   "CREATE INDEX conv_topic_group ON conversations_topic_group (group_id);"
                                   );

    public void migrate(DBConnection connection) throws Exception {
        for (String ddl : INDEXES.split(";")) {
            if (ddl.trim().isEmpty()) {
                continue;
            }

            connection.run(ddl.trim()).executeUpdate();
        }
    }

}
