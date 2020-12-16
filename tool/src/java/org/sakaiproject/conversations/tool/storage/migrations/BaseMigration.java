package org.sakaiproject.conversations.tool.storage.migrations;

import org.sakaiproject.conversations.tool.storage.*;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import java.sql.SQLException;

abstract public class BaseMigration {

    final static String MIGRATION_TABLE = "conversations_schema_info";
    final static String MIGRATION_PKG = "org.sakaiproject.conversations.tool.storage.migrations";

    public static void runMigrations() {
        try {
            List<BaseMigration> migrations = getSortedMigrations();

            DB.transaction("Run database migrations",
                           (DBConnection db) -> {
                               try {
                                   db.run("create table " + MIGRATION_TABLE + " (version number)").executeUpdate();
                                   db.run("insert into " + MIGRATION_TABLE + " values (0)").executeUpdate();
                                   db.commit();
                               } catch (SQLException e) {
                                   // Expected to fail unless this is the first run.
                               };

                               // Get our last version
                               int version = 0;
                               try (DBResults row = db.run("select version from " + MIGRATION_TABLE).executeQuery()) {
                                   if (row.hasNext()) {
                                       version = row.next().getInt("version");
                                   }
                               }

                               System.err.println("*** Running migrations from " + version);

                               for (BaseMigration migration : migrations) {
                                   if (migration.getVersion() <= version && !migration.alwaysRun()) {
                                       continue;
                                   }

                                   System.err.println(String.format("*** Running migration: %s", migration));
                                   try {
                                       migration.migrate(db);
                                       if (!migration.alwaysRun()) {
                                           version = migration.getVersion();
                                           db.run("update " + MIGRATION_TABLE + " set version = ?").param(version).executeUpdate();
                                           db.commit();
                                       }
                                   } catch (Exception migrationError) {
                                       System.err.println(String.format("*** ERROR IN DB MIGRATION %s: %s",
                                                                        migration,
                                                                        migrationError));
                                       db.rollback();
                                       throw new RuntimeException(migrationError);
                                   }
                               }

                               db.commit();

                               return null;
                           });
        } catch (Exception e) {
            System.err.println("*** ERROR IN DB MIGRATION: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static List<BaseMigration> getSortedMigrations() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<java.net.URL> resources = loader.getResources(MIGRATION_PKG.replace(".", "/"));

        List<BaseMigration> migrations = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL elt = resources.nextElement();

            if (!"file".equals(elt.getProtocol())) {
                continue;
            }

            // Search our classpath for any migration-looking classes
            List<String> migrationClasses = new ArrayList<>();
            for (File f : new File(elt.getPath()).listFiles()) {
                if (f.getName().matches("V[0-9]+_.*\\.class")) {
                    migrationClasses.add(f.getName().replaceAll("\\.class$", ""));
                }
            }

            // Sort migrations numerically
            Collections.sort(migrationClasses, (s1, s2) -> {
                    int n1 = Integer.valueOf(s1.substring(1).split("_")[0]);
                    int n2 = Integer.valueOf(s2.substring(1).split("_")[0]);

                    return (n1 - n2);
                });

            // Create our instances
            for (String migrationClassName : migrationClasses) {
                Class<?> migrationClass = Class.forName(MIGRATION_PKG + "." + migrationClassName);
                migrations.add((BaseMigration) migrationClass.getConstructor().newInstance());
            }

            // Migrations must be strictly increasing
            for (int i = 0; i < migrations.size() - 1; i++) {
                if (migrations.get(i).getVersion() == migrations.get(i + 1).getVersion()) {
                    throw new RuntimeException("Version number repeated in migrations: " + migrations.get(i).getVersion());
                }
            }
        }

        return migrations;
    }


    private Integer getVersion() {
        String name = this.getClass().getName().replaceAll("^.*\\.", "");
        return Integer.valueOf(name.substring(1).split("_")[0]);
    }

    abstract void migrate(DBConnection db) throws Exception;

    // If a migration is marked as "alwaysRun", it gets run on every invocation.
    public boolean alwaysRun() {
        return false;
    }
}
