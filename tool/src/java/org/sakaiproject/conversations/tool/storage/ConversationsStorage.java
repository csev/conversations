/**********************************************************************************
 *
 * Copyright (c) 2019 The Sakai Foundation
 *
 * Original developers:
 *
 *   New York University
 *   Payten Giles
 *   Mark Triggs
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.conversations.tool.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.conversations.tool.models.Attachment;
import org.sakaiproject.conversations.tool.models.MissingUuidException;
import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.models.Poster;
import org.sakaiproject.conversations.tool.models.Topic;
import org.sakaiproject.conversations.tool.models.TopicSettings;
import org.sakaiproject.conversations.tool.storage.migrations.BaseMigration;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;


@Slf4j
public class ConversationsStorage {

    public void runDBMigrations() {
        BaseMigration.runMigrations();
    }


    public void storeFileMetadata(final String key, final String mimeType, final String fileName, final String role) {
        DB.transaction
            ("Store file metadata",
             (DBConnection db) -> {
                String id = UUID.randomUUID().toString();

                Long createdAt = System.currentTimeMillis();

                db.run(
                       "INSERT INTO conversations_files (uuid, mime_type, filename, role)" +
                       " VALUES (?, ?, ?, ?)")
                    .param(key)
                    .param(mimeType)
                    .param(fileName)
                    .param(role)
                    .executeUpdate();

                db.commit();

                return null;
            });
    }

    public Optional<Attachment> readFileMetadata(final String key) {
        return DB.transaction
            ("Retrieve file metadata",
             (DBConnection db) -> {
                try (DBResults results = db.run("select * from conversations_files where uuid = ?")
                     .param(key)
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        return (Optional<Attachment>)Optional.of(new Attachment(key,
                                                          result.getString("mime_type"),
                                                          result.getString("filename"),
                                                          result.getString("role")));
                    }
                }

                Optional<Attachment> result = Optional.empty();
                return result;
            });
    }

    public List<Topic> getTopics(final String siteId, final Integer page, final Integer pageSize, final String orderBy, final String orderDirection) {
        return DB.transaction
            ("Get topics on page for site",
             (DBConnection db) -> {
                List<Topic> topics = new ArrayList<>();

                final int minRowNum = pageSize * page + 1;
                final int maxRowNum = pageSize * page + pageSize;

                try (DBResults results = db.run(
                                                "SELECT *" +
                                                " FROM conversations_topic" +
                                                " INNER JOIN conversations_topic_settings ON conversations_topic_settings.topic_uuid = conversations_topic.uuid" +
                                                " WHERE uuid in (" +
                                                "   SELECT uuid FROM (" +
                                                "     SELECT uuid, rownum rnk FROM (" +
                                                "       SELECT uuid FROM conversations_topic" +
                                                "       WHERE site_id = ?" +
                                                "       ORDER BY " + forceOneOf(orderBy, Arrays.asList("title", "type", "last_activity_at"), "last_activity_at") + " " + forceOneOf(orderDirection.toUpperCase(), Arrays.asList("ASC", "DESC"), "DESC") +
                                                "     )" +
                                                "   ) WHERE rnk BETWEEN ? AND ?" +
                                                " )" +
                                                " ORDER BY " + forceOneOf(orderBy, Arrays.asList("title", "type", "last_activity_at"), "last_activity_at") + " " + forceOneOf(orderDirection.toUpperCase(), Arrays.asList("ASC", "DESC"), "DESC"))
                     .param(siteId)
                     .param(String.valueOf(minRowNum))
                     .param(String.valueOf(maxRowNum))
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        String topicUuid = result.getString("uuid");

                        Topic topic = new Topic(topicUuid,
                                                result.getString("title"),
                                                result.getString("type"),
                                                result.getString("created_by"),
                                                result.getLong("created_at"),
                                                result.getLong("last_activity_at"));

                        TopicSettings settings = new TopicSettings(topicUuid,
                                result.getString("availability"),
                                result.getInt("published") == 1,
                                result.getInt("graded") == 1,
                                result.getInt("allow_comments") == 1,
                                result.getInt("allow_like") == 1,
                                result.getInt("require_post") == 1);

                        topic.setSettings(settings);

                        if ("GROUPS".equals(settings.getAvailability())) {
                            try {
                                Site site = SiteService.getSite(siteId);
                                Collection<Group> groups = site.getGroups();
                                try (DBResults groupResults = db.run(
                                    "SELECT * from conversations_topic_group" +
                                    " WHERE topic_uuid = ?")
                                    .param(topicUuid)
                                    .executeQuery()) {
                                    for (ResultSet groupResult : groupResults) {
                                        String groupRef = groupResult.getString("group_id");
                                        settings.getGroups().add(groupRef);
                                        for (Group group : groups) {
                                            if (groupRef.endsWith(group.getId())) {
                                                settings.getGroupIdToName().put(groupResult.getString("group_id"), group.getTitle());
                                            }
                                        }
                                    }
                                }
                            } catch (IdUnusedException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }

                        topics.add(topic);
                    }

                    return topics;
                }
            });
    }

    public List<Topic> getTopicsForStudent(final String siteId, final Integer page, final Integer pageSize, final String orderBy, final String orderDirection, final List<String> groupIds) {
        // FIXME pagination
        return DB.transaction
                ("Get topics on page for site for student",
                        (DBConnection db) -> {
                            List<Topic> topics = new ArrayList<>();

                            final int minRowNum = pageSize * page + 1;
                            final int maxRowNum = pageSize * page + pageSize;

                            String groupIdPlaceholders = groupIds.stream().map(_p -> "?").collect(Collectors.joining(","));

                            // FIXME handle no groups!
                            if (groupIds.isEmpty()) {
                                groupIdPlaceholders = "'__NO_GROUPS_TO_MATCH__'";
                            }

                            try (PreparedStatement ps = db.prepareStatement(
                                    "SELECT *" +
                                            " FROM conversations_topic" +
                                            " INNER JOIN conversations_topic_settings ON conversations_topic_settings.topic_uuid = conversations_topic.uuid" +
                                            " WHERE uuid in (" +
                                            "   SELECT uuid FROM (" +
                                            "     SELECT uuid, rownum rnk FROM (" +
                                            "       SELECT uuid FROM conversations_topic" +
                                            "       INNER JOIN conversations_topic_settings ON conversations_topic_settings.topic_uuid = conversations_topic.uuid" +
                                            "       WHERE conversations_topic.site_id = ?" +
                                            "       AND conversations_topic_settings.published = 1" +
                                            "       AND (" +
                                            "         conversations_topic_settings.availability = 'ENTIRE_SITE'" +
                                            "         OR (" +
                                            "           conversations_topic_settings.availability = 'GROUPS' AND conversations_topic.uuid IN (" +
                                            "             SELECT topic_uuid FROM conversations_topic_group WHERE group_id in (" + groupIdPlaceholders + ")" +
                                            "           )" +
                                            "         )" +
                                            "       )" +
                                            "       ORDER BY " + forceOneOf(orderBy, Arrays.asList("title", "type", "last_activity_at"), "last_activity_at") + " " + forceOneOf(orderDirection.toUpperCase(), Arrays.asList("ASC", "DESC"), "DESC") +
                                            "     )" +
                                            "   ) WHERE rnk BETWEEN ? AND ?" +
                                            " )" +
                                            " ORDER BY " + forceOneOf(orderBy, Arrays.asList("title", "type", "last_activity_at"), "last_activity_at") + " " + forceOneOf(orderDirection.toUpperCase(), Arrays.asList("ASC", "DESC"), "DESC"))) {

                                int index = 1;
                                ps.setString(index, siteId);
                                Iterator<String> it = groupIds.iterator();
                                while (it.hasNext()) {
                                    index += 1;
                                    ps.setString(index, it.next());
                                }
                                index += 1;
                                ps.setInt(index, minRowNum);
                                index += 1;
                                ps.setInt(index, maxRowNum);

                                try (ResultSet rs = ps.executeQuery()) {
                                    while (rs.next()) {
                                        Topic topic = new Topic(rs.getString("uuid"),
                                                rs.getString("title"),
                                                rs.getString("type"),
                                                rs.getString("created_by"),
                                                rs.getLong("created_at"),
                                                rs.getLong("last_activity_at"));

                                        TopicSettings settings = new TopicSettings(rs.getString("uuid"),
                                                rs.getString("availability"),
                                                rs.getInt("published") == 1,
                                                rs.getInt("graded") == 1,
                                                rs.getInt("allow_comments") == 1,
                                                rs.getInt("allow_like") == 1,
                                                rs.getInt("require_post") == 1);

                                        topic.setSettings(settings);

                                        topics.add(topic);
                                    }

                                    return topics;
                                }
                            }
                        });
    }

    private String forceOneOf(String value, List<String> validValues, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (validValues.contains(value)) {
            return value;
        } else {
            return defaultValue;
        }
    }

    public Integer getTopicsCount(final String siteId) {
        return DB.transaction
            ("Count topics for site",
             (DBConnection db) -> {
                Integer count = 0;
                try (DBResults results = db.run("SELECT count(*) as count FROM conversations_topic WHERE site_id = ?")
                     .param(siteId)
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        count = result.getInt("count");
                    }
                }
                return count;
            });
    }

    public Integer getTopicsForStudentCount(final String siteId, final List<String> groupIds) {
        return DB.transaction
                ("Count topics for student in site",
                        (DBConnection db) -> {
                            Integer count = 0;
                            String groupIdPlaceholders = groupIds.stream().map(_p -> "?").collect(Collectors.joining(","));
                            // FIXME handle no groups!
                            if (groupIds.isEmpty()) {
                                groupIdPlaceholders = "'__NO_GROUPS_TO_MATCH__'";
                            }
                            try (PreparedStatement ps = db.prepareStatement(
                                "SELECT count(*) as count" +
                                        " FROM conversations_topic" +
                                        " INNER JOIN conversations_topic_settings ON conversations_topic_settings.topic_uuid = conversations_topic.uuid" +
                                        " WHERE conversations_topic.site_id = ?" +
                                        " AND conversations_topic_settings.published = 1" +
                                        " AND (" +
                                        "   conversations_topic_settings.availability = 'ENTIRE_SITE'" +
                                        "   OR (" +
                                        "     conversations_topic_settings.availability = 'GROUPS'" +
                                        "     AND conversations_topic.uuid IN (" +
                                        "       SELECT topic_uuid FROM conversations_topic_group WHERE group_id in (" + groupIdPlaceholders + ")" +
                                        "     )" +
                                        "   )" +
                                        " )"
                            )) {
                                int index = 1;
                                ps.setString(index, siteId);
                                Iterator<String> it = groupIds.iterator();
                                while (it.hasNext()) {
                                    index += 1;
                                    ps.setString(index, it.next());
                                }
                                try (ResultSet rs = ps.executeQuery()) {
                                    while (rs.next()) {
                                        count = rs.getInt("count");
                                    }
                                }
                            }

                            return count;
                        });
    }

    public void touchTopicLastActivityAt(final String topicUuid) {
        touchTopicLastActivityAt(topicUuid, System.currentTimeMillis());
    }

    public void touchTopicLastActivityAt(final String topicUuid, final Long lastActivityAt) {
        DB.transaction
            ("Create a post for a topic",
             (DBConnection db) -> {
                db.run("UPDATE conversations_topic SET last_activity_at = ?" +
                       " WHERE uuid = ?")
                    .param(lastActivityAt)
                    .param(topicUuid)
                    .executeUpdate();

                db.commit();

                return null;
            });
    }

    public Map<String, List<Poster>> getPostersForTopics(final List<String> topicUuids) {
        return DB.transaction
            ("Find all posters for topics",
             (DBConnection db) -> {
                Map<String, List<Poster>> postersByTopic = new HashMap();

                String placeholders = topicUuids.stream().map(_p -> "?").collect(Collectors.joining(","));

                try (PreparedStatement ps = db.prepareStatement(
                                                                "SELECT poster.*, sakai_user_id_map.eid, nyu_t_users.fname, nyu_t_users.lname" +
                                                                " FROM (SELECT posted_by, topic_uuid, MAX(posted_at) AS latest_posted_at" +
                                                                "       FROM conversations_post" + 
                                                                "       WHERE topic_uuid in (" + placeholders + ")" +
                                                                "       GROUP BY posted_by, topic_uuid) poster" +
                                                                " INNER JOIN sakai_user_id_map ON sakai_user_id_map.user_id = poster.posted_by" + 
                                                                " LEFT JOIN nyu_t_users ON nyu_t_users.netid = sakai_user_id_map.eid" +
                                                                " ORDER BY poster.latest_posted_at DESC")) {
                    Iterator<String> it = topicUuids.iterator();
                    for (int i = 0; it.hasNext(); i++) {
                        ps.setString(i + 1, it.next());
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String topicUuid = rs.getString("topic_uuid");
                            if (!postersByTopic.containsKey(topicUuid)) {
                                postersByTopic.put(topicUuid, new ArrayList<Poster>());
                            }
                            postersByTopic.get(topicUuid).add(new Poster(rs.getString("posted_by"),
                                                                         rs.getString("eid"),
                                                                         rs.getString("fname"),
                                                                         rs.getString("lname"),
                                                                         rs.getLong("latest_posted_at")));
                        }
                    }
                }

                return postersByTopic;
            });
    }

    public Map<String, Long> getPostCountsForTopics(final List<String> topicUuids) {
        // FIXME ORDER BY MOST RECENT CHANGES
        return DB.transaction
            ("Find post counts for topics",
             (DBConnection db) -> {
                Map<String, Long> postCountsByTopic = new HashMap();

                String placeholders = topicUuids.stream().map(_p -> "?").collect(Collectors.joining(","));

                try (PreparedStatement ps = db.prepareStatement("SELECT count(*) as count, topic_uuid FROM conversations_post WHERE topic_uuid in (" + placeholders + ") GROUP BY topic_uuid")) {
                    Iterator<String> it = topicUuids.iterator();
                    for (int i = 0; it.hasNext(); i++) {
                        ps.setString(i + 1, it.next());
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String topicUuid = rs.getString("topic_uuid");
                            Long count = rs.getLong("count");
                            postCountsByTopic.put(topicUuid, count);
                        }
                    }
                }

                return postCountsByTopic;
            });
    }

    public Map<String, Long> getLastActivityTimeForTopics(final List<String> topicUuids) {
        return DB.transaction
            ("Find last activity times for topics",
             (DBConnection db) -> {
                Map<String, Long> lastActivityByTopic = new HashMap();

                String placeholders = topicUuids.stream().map(_p -> "?").collect(Collectors.joining(","));

                try (PreparedStatement ps = db.prepareStatement("SELECT max(posted_at) as last_activity, topic_uuid FROM conversations_post WHERE topic_uuid in (" + placeholders + ") GROUP BY topic_uuid")) {
                    Iterator<String> it = topicUuids.iterator();
                    for (int i = 0; it.hasNext(); i++) {
                        ps.setString(i + 1, it.next());
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String topicUuid = rs.getString("topic_uuid");
                            Long count = rs.getLong("last_activity");
                            lastActivityByTopic.put(topicUuid, count);
                        }
                    }
                }

                return lastActivityByTopic;
            });
    }

    public String createTopic(Topic topic, String siteId, String userId, TopicSettings settings) {
        return DB.transaction
            ("Create a topic for a site",
             (DBConnection db) -> {
                 String id = UUID.randomUUID().toString();

                 Long createdAt = System.currentTimeMillis();

                 db.run(
                        "INSERT INTO conversations_topic (uuid, title, type, site_id, created_by, created_at, last_activity_at)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?)")
                     .param(id)
                     .param(topic.getTitle())
                     .param(topic.getType())
                     .param(siteId)
                     .param(userId)
                     .param(createdAt)
                     .param(createdAt)
                     .executeUpdate();

                 db.run(
                         "INSERT INTO conversations_topic_settings (topic_uuid, availability, published, graded, allow_comments, allow_like, require_post)" +
                         " VALUES (?, ?, ?, ?, ?, ?, ?)")
                     .param(id)
                     .param(settings.getAvailability())
                     .param(settings.isPublished() ? 1 : 0)
                     .param(settings.isGraded() ? 1 : 0)
                     .param(settings.isAllowComments() ? 1 : 0)
                     .param(settings.isAllowLike() ? 1 : 0)
                     .param(settings.isRequirePost() ? 1 : 0)
                     .executeUpdate();

                for(String groupId : settings.getGroups()) {
                    db.run(
                           "INSERT INTO conversations_topic_group (topic_uuid, group_id)" +
                           " VALUES (?, ?)")
                        .param(id)
                        .param(groupId)
                        .executeUpdate();
                }

                db.commit();

                return id;
            });
    }

    public String updateTopicSettings(final String topicUuid, TopicSettings settings) {
        return DB.transaction
                ("Update topic settings",
                        (DBConnection db) -> {
                            db.run(
                                    "UPDATE conversations_topic_settings" +
                                     " SET availability = ?, published = ?, graded = ?, allow_comments = ?, allow_like = ?, require_post = ?" +
                                     " WHERE topic_uuid = ?")
                                .param(settings.getAvailability())
                                .param(settings.isPublished() ? 1 : 0)
                                .param(settings.isGraded() ? 1 : 0)
                                .param(settings.isAllowComments() ? 1 : 0)
                                .param(settings.isAllowLike() ? 1 : 0)
                                .param(settings.isRequirePost() ? 1 : 0)
                                .param(topicUuid)
                                .executeUpdate();

                            db.run("DELETE FROM conversations_topic_group WHERE topic_uuid = ?")
                                .param(topicUuid)
                                .executeUpdate();

                            for(String groupId : settings.getGroups()) {
                                db.run(
                                       "INSERT INTO conversations_topic_group (topic_uuid, group_id)" +
                                       " VALUES (?, ?)")
                                    .param(topicUuid)
                                    .param(groupId)
                                    .executeUpdate();
                            }

                            db.commit();

                            return topicUuid;
                        });
    }

    public Optional<Topic> getTopic(String uuid, final String siteId) {
        return DB.transaction
            ("Find a topic by uuid for a site",
             (DBConnection db) -> {
                try (DBResults results = db.run(
                    "SELECT * from conversations_topic" +
                        " INNER JOIN conversations_topic_settings ON conversations_topic_settings.topic_uuid = conversations_topic.uuid" +
                        " WHERE conversations_topic.uuid = ? AND conversations_topic.site_id = ?")
                     .param(uuid)
                     .param(siteId)
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        TopicSettings settings = new TopicSettings(result.getString("uuid"),
                                                                   result.getString("availability"),
                                                                   result.getInt("published") == 1,
                                                                   result.getInt("graded") == 1,
                                                                   result.getInt("allow_comments") == 1,
                                                                   result.getInt("allow_like") == 1,
                                                                   result.getInt("require_post") == 1);

                        try (DBResults groupResults = db.run(
                                    "SELECT * from conversations_topic_group" +
                                    " WHERE topic_uuid = ?")
                                .param(uuid)
                                .executeQuery()){
                            for (ResultSet groupResult : groupResults) {
                                settings.getGroups().add(groupResult.getString("group_id"));
                            }
                        }

                        Topic topic = new Topic(result.getString("uuid"),
                                                result.getString("title"),
                                                result.getString("type"),
                                                result.getString("created_by"),
                                                result.getLong("created_at"),
                                                result.getLong("last_activity_at"));

                        topic.setSettings(settings);

                        return Optional.of(topic);
                    }

                    Optional<Topic> result = Optional.empty();
                    return result;
                }
            });
    }

    private void loadAttachments(List<Post> posts) {
        if (posts.isEmpty()) {
            return;
        }

        DB.transaction
            ("Fetch attachments for posts",
             (DBConnection db) -> {
                Map<String, Post> postMap = new HashMap<>();
                for (Post p : posts) {
                    try {
                        postMap.put(p.getUuid(), p);
                    } catch (MissingUuidException e) {}
                }

                int pageSize = 200;
                List<String> workSet = new ArrayList<>(pageSize);
                for (int start = 0; start < posts.size();) {
                    int end = Math.min(start + pageSize, posts.size());
                    workSet.clear();

                    for (int i = start; i < end; i++) {
                        try {
                            workSet.add(posts.get(i).getUuid());
                        } catch (MissingUuidException e) {}
                    }

                    String placeholders = workSet.stream().map(_p -> "?").collect(Collectors.joining(","));

                    DBPreparedStatement ps = db.run("SELECT a.post_uuid, f.*" +
                                                    " FROM conversations_attachments a " +
                                                    " INNER JOIN conversations_files f on f.uuid = a.attachment_key" +
                                                    " WHERE a.post_uuid in (" + placeholders + ")");

                    for (String postUuid : workSet) {
                        ps.param(postUuid);
                    }

                    try (DBResults results = ps.executeQuery()) {
                        for (ResultSet result : results) {
                            Post p = postMap.get(result.getString("post_uuid"));

                            p.addAttachment(new Attachment(result.getString("uuid"),
                                                           result.getString("mime_type"),
                                                           result.getString("filename"),
                                                           result.getString("role")));
                        }
                    }

                    start = end;
                }

                return null;
            });
    }

    public List<Post> getPosts(final String topicUuid) {
        return DB.transaction
            ("Find all posts for topic",
             (DBConnection db) -> {
                List<Post> posts = new ArrayList<>();
                try (DBResults results = db.run("SELECT conversations_post.*, sakai_user_id_map.eid, nyu_t_users.fname, nyu_t_users.lname FROM conversations_post" +
                                                " INNER JOIN sakai_user_id_map ON sakai_user_id_map.user_id = conversations_post.posted_by" +
                                                " LEFT JOIN nyu_t_users ON nyu_t_users.netid = sakai_user_id_map.eid" +
                                                " WHERE conversations_post.topic_uuid = ?")
                     .param(topicUuid)
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        String postUuid = result.getString("uuid");
                        Post post = new Post(
                                postUuid,
                                result.getString("content"),
                                result.getString("posted_by"),
                                result.getLong("posted_at"),
                                result.getString("parent_post_uuid"),
                                result.getString("eid"),
                                result.getString("fname"),
                                result.getString("lname"),
                                result.getLong("version"));

                        try (DBResults likeResults = db.run("SELECT * FROM conversations_post_like" +
                                                            " WHERE post_uuid = ?")
                                .param(postUuid)
                                .executeQuery()) {
                            for (ResultSet likeResult : likeResults) {
                                post.getLikedBy().add(likeResult.getString("user_id"));
                            }
                        }

                        posts.add(post);
                    }

                    loadAttachments(posts);

                    return posts;
                }
            });
    }

    public Post getPost(final String postUuid) {
        return DB.transaction
            ("Find all posts for topic",
             (DBConnection db) -> {
                Post post = null;
                try (DBResults results = db.run("SELECT conversations_post.*, sakai_user_id_map.eid, nyu_t_users.fname, nyu_t_users.lname FROM conversations_post" +
                                                " INNER JOIN sakai_user_id_map ON sakai_user_id_map.user_id = conversations_post.posted_by" +
                                                " LEFT JOIN nyu_t_users ON nyu_t_users.netid = sakai_user_id_map.eid" +
                                                " WHERE conversations_post.uuid = ?")
                     .param(postUuid)
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        post = new Post(result.getString("uuid"),
                                        result.getString("content"),
                                        result.getString("posted_by"),
                                        result.getLong("posted_at"),
                                        result.getString("parent_post_uuid"),
                                        result.getString("eid"),
                                        result.getString("fname"),
                                        result.getString("lname"),
                                        result.getLong("version"));
                    }

                    loadAttachments(Arrays.asList(post));

                    return post;
                }
            });
    }

    public String createPost(Post post, String topicUuid) {
        return createPost(post, topicUuid, null, Collections.emptyList(), System.currentTimeMillis());
    }

    public String createPost(final Post post, final String topicUuid, final String parentPostUuid, final List<String> attachmentKeys, long postedAt) {
        return DB.transaction
            ("Create a post for a topic",
             (DBConnection db) -> {
                String id = UUID.randomUUID().toString();

                db.run("INSERT INTO conversations_post (uuid, topic_uuid, parent_post_uuid, content, posted_by, posted_at, updated_by, updated_at, version)" +
                       " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .param(id)
                    .param(topicUuid)
                    .param(parentPostUuid)
                    .param(post.getContent())
                    .param(post.getPostedBy())
                    .param(postedAt)
                    .param(post.getPostedBy())
                    .param(postedAt)
                    .param(1)
                    .executeUpdate();

                touchTopicLastActivityAt(topicUuid, postedAt);

                for (String attachmentKey : attachmentKeys) {
                    db.run("INSERT INTO conversations_attachments (uuid, post_uuid, attachment_key) VALUES (?, ?, ?)")
                        .param(UUID.randomUUID().toString())
                        .param(id)
                        .param(attachmentKey)
                        .executeUpdate();
                }

                db.commit();

                return id;
            });
    }

    public String updatePost(final Post post, final String topicUuid, final List<String> attachmentKeys, long postedAt) {
        return DB.transaction
            ("Create a post for a topic",
             (DBConnection db) -> {
                try {
                    db.run("UPDATE conversations_post SET content = ?, updated_by = ?, updated_at = ?, version = ? WHERE uuid = ? AND topic_uuid = ?")
                        .param(post.getContent())
                        .param(post.getUpdatedBy())
                        .param(postedAt)
                        .param(post.getVersion())
                        .param(post.getUuid())
                        .param(topicUuid)
                        .executeUpdate();

                    touchTopicLastActivityAt(topicUuid, postedAt);

                    for (String attachmentKey : attachmentKeys) {
                        db.run("DELETE FROM conversations_attachments WHERE post_uuid = ?")
                            .param(post.getUuid())
                            .executeUpdate();

                        db.run("INSERT INTO conversations_attachments (uuid, post_uuid, attachment_key) VALUES (?, ?, ?)")
                            .param(UUID.randomUUID().toString())
                            .param(post.getUuid())
                            .param(attachmentKey)
                            .executeUpdate();
                    }

                    db.commit();

                    return post.getUuid();
                } catch(MissingUuidException e) {
                    // FIXME
                    throw new RuntimeException("Unable to update post due to missing uuid");
                }
            });
    }

    public String setLastReadTopicEvent(final String topicUuid, final String userId) {
        return DB.transaction
            ("Create or update an event for reading a topic",
             (DBConnection db) -> {
                String id = UUID.randomUUID().toString();
                Long timestamp = System.currentTimeMillis();

                db.run("DELETE FROM conversations_topic_event WHERE topic_uuid = ? AND user_id = ? AND event_name = ?")
                    .param(topicUuid)
                    .param(userId)
                    .param("TOPIC_LAST_READ")
                    .executeUpdate();

                db.run("INSERT INTO conversations_topic_event (uuid, topic_uuid, user_id, event_name, event_time) VALUES (?, ?, ?, ?, ?)")
                    .param(id)
                    .param(topicUuid)
                    .param(userId)
                    .param("TOPIC_LAST_READ")
                    .param(timestamp)
                    .executeUpdate();

                db.commit();

                return id;
            });
    }

    public Long getLastReadTopic(String topicUuid, String userId) {
        return DB.transaction
            ("Get time user last read a topic",
             (DBConnection db) -> {
                try (DBResults results = db.run("SELECT * from conversations_topic_event WHERE topic_uuid = ? AND user_id = ? AND event_name = ? ORDER BY event_time DESC")
                     .param(topicUuid)
                     .param(userId)
                     .param("TOPIC_LAST_READ")
                     .executeQuery()) {
                    for (ResultSet result : results) {
                        return result.getLong("event_time");
                    }

                    return 0L;
                }
            });
    }

    public void likePost(final String postUuid, final String userId, final boolean like) {
        DB.transaction
                ("Like a post",
                        (DBConnection db) -> {
                            db.run("DELETE FROM conversations_post_like WHERE post_uuid = ? AND user_id = ?")
                                    .param(postUuid)
                                    .param(userId)
                                    .executeUpdate();
                            if (like) {
                                db.run(
                                        "INSERT INTO conversations_post_like (post_uuid, user_id)" +
                                                " VALUES (?, ?)")
                                        .param(postUuid)
                                        .param(userId)
                                        .executeUpdate();
                            }

                            db.commit();

                            return null;
                        });
    }
}
