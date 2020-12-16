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

package org.sakaiproject.conversations.tool.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import org.sakaiproject.conversations.tool.ConversationsServlet;
import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.models.Topic;
import org.sakaiproject.conversations.tool.models.TopicSettings;
import org.sakaiproject.conversations.tool.storage.ConversationsStorage;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.site.api.Site;

public class CreateTopicHandler implements Handler {

    private String redirectTo = null;

    public CreateTopicHandler() {
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            String siteId = (String)context.get("siteId");

            String title = p.getString("title", null);
            String type = p.getString("type", null);
            String initialPost = p.getString("post", null);

            if (title == null || type == null) {
                // FIXME
                throw new RuntimeException("tile and type required");
            }

            Topic topic = new Topic(title, type);

            TopicSettings topicSettings = new TopicSettings(
                    null,
                    p.getString("settings[availability]", TopicSettings.AVAILABILITY_ENTIRE_SITE),
                    "true".equals(p.getString("settings[published]", null)),
                    "true".equals(p.getString("settings[graded]", null)),
                    "true".equals(p.getString("settings[allow_comments]", null)),
                    "true".equals(p.getString("settings[allow_like]", null)),
                    "true".equals(p.getString("settings[require_post]", null)));

            topicSettings.setGroups(p.getStrings("settings[groups][]"));

            User currentUser = UserDirectoryService.getCurrentUser();

            String topicUuid = new ConversationsStorage().createTopic(topic, siteId, currentUser.getId(), topicSettings);

            if (initialPost != null) {
                if (initialPost.startsWith("<p>#IMPORTDEMO")) {
                    demoImport(initialPost, topicUuid, siteId);
                } else {
                    Post post = new Post(initialPost, currentUser.getId());

                    String postUuid = new ConversationsStorage().createPost(post, topicUuid);
                }
            }

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("topicUuid", topicUuid);
            response.getWriter().write(result.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void demoImport(String text, String topicUuid, String siteId) throws Exception {
        String[] messages = text.split("</p>");

        String importSpec = messages[0];

        int days = 12;


        Pattern daysPattern = Pattern.compile(".*days=([0-9]+).*");
        Matcher m = daysPattern.matcher(importSpec);

        if (m.matches()) {
            days = Integer.valueOf(m.group(1));
        }

        List<Long> times = new ArrayList<>();
        for (String line : messages) {
            if (line.indexOf(":") < 0) {
                continue;
            }

            times.add(System.currentTimeMillis() - ((long)Math.floor(Math.random() * (days * 86400000))));
        }

        Collections.sort(times);

        Map<String, String> characters = new HashMap<>();
        for (String line : messages) {
            if (line.indexOf(":") < 0) {
                continue;
            }

            String[] bits = line.replace("<p>", "").split(":", 2);

            if (!characters.containsKey(bits[0])) {
                // Create the user
                String userUuid = UUID.randomUUID().toString();
                UserDirectoryService.addUser(userUuid,
                                             bits[0].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase(java.util.Locale.ROOT) + "_" + UUID.randomUUID().toString(),
                                             "",
                                             bits[0],
                                             "example@dishevelled.net",
                                             "testuser",
                                             "registered",
                                             null);

                characters.put(bits[0], userUuid);
            }

            long postTime = times.remove(0);

            String postUuid = new ConversationsStorage().createPost(new Post(bits[1].trim(), characters.get(bits[0])),
                                                                    topicUuid,
                                                                    null,
                                                                    Collections.emptyList(),
                                                                    postTime);
        }

        Site site = SiteService.getSite(siteId);
        for (String userId : characters.values()) {
            if ("course".equals(site.getType())) {
                site.addMember(userId, "Student", true, false);
            } else {
                site.addMember(userId, "access", true, false);
            }
        }

        SiteService.save(site);
    }

    public boolean hasRedirect() {
        return (redirectTo != null);
    }

    public String getRedirect() {
        return redirectTo;
    }

    public Errors getErrors() {
        return null;
    }

    public Map<String, List<String>> getFlashMessages() {
        return new HashMap<String, List<String>>();
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public boolean hasTemplate() {
        return false;
    }

    @Override
    public boolean isRolePermitted(final String role) {
        return ConversationsServlet.ROLE_INSTRUCTOR.equals(role);
    }
}
