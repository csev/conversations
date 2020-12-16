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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sakaiproject.conversations.tool.ConversationsServlet;
import org.sakaiproject.conversations.tool.models.Poster;
import org.sakaiproject.conversations.tool.models.Topic;
import org.sakaiproject.conversations.tool.storage.ConversationsStorage;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class TopicFeedHandler implements Handler {

    public static Integer PAGE_SIZE = 20;

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            String topicUuid = p.getString("topicUuid", null);
            String siteId = (String)context.get("siteId");

            if (topicUuid == null) {
                throw new RuntimeException("topicUuid required");
            }

            // FIXME lock this down to instructors only

            ConversationsStorage storage = new ConversationsStorage();

            Optional<Topic> topic = storage.getTopic(topicUuid, siteId);

            if (!topic.isPresent()) {
                // FIXME
                throw new RuntimeException("Topic not found for uuid");
            }

            Site site = SiteService.getSite(siteId);
            Collection<Group> groups = site.getGroups();
            JSONArray groupsJSON = new JSONArray();
            for (Group group : groups) {
                JSONObject groupJSON = new JSONObject();
                groupJSON.put("name", group.getTitle());
                groupJSON.put("reference", group.getReference());
                ResourceProperties groupProperties = group.getProperties();
                if (groupProperties.get("sections_eid") != null) {
                    groupJSON.put("type", "section");
                } else {
                    groupJSON.put("type", "group");
                }
                groupsJSON.add(groupJSON);
            }

            JSONObject result = new JSONObject();
            result.put("available_groups", groupsJSON);
            result.put("topic", topic.get().asJSONObject());

            response.getWriter().write(result.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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