package org.sakaiproject.conversations.tool.storage.migrations;

import org.sakaiproject.conversations.tool.storage.DB;
import org.sakaiproject.conversations.tool.storage.DBConnection;

public class V3__AlwaysRuns extends BaseMigration {

    public void migrate(DBConnection connection) throws Exception {
        System.err.println("I don't do much but I'm always here.");
    }

    public boolean alwaysRun() {
        return true;
    }
}
