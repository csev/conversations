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
import org.sakaiproject.conversations.tool.models.MissingUuidException;
import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.models.Poster;
import org.sakaiproject.conversations.tool.models.Topic;
import org.sakaiproject.conversations.tool.storage.ConversationsStorage;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class TopicsFeedHandler implements Handler {

    public static Integer PAGE_SIZE = 20;

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            Integer page = Integer.valueOf(p.getString("page", "0"));

            String orderBy = p.getString("order_by", "last_activity_at");
            String orderDirection = p.getString("order_direction", "desc");

            String siteId = (String)context.get("siteId");

            ConversationsStorage storage = new ConversationsStorage();

            List<Topic> topics = new ArrayList<Topic>();
            Integer topicsCount = 0;

            if ((Boolean) context.get("isStudent")) {
                List<String> userGroupIds = new ArrayList<String>();
                User currentUser = UserDirectoryService.getCurrentUser();
                Site site = SiteService.getSite(siteId);
                for (Group group : site.getGroupsWithMember(currentUser.getId())) {
                    userGroupIds.add(group.getReference());
                }

                topics = storage.getTopicsForStudent(siteId, page, PAGE_SIZE, orderBy, orderDirection, userGroupIds);
                topicsCount = storage.getTopicsForStudentCount(siteId, userGroupIds);
            } else {
                topics = storage.getTopics(siteId, page, PAGE_SIZE, orderBy, orderDirection);
                topicsCount = storage.getTopicsCount(siteId);
            }
            List<String> topicUuids = new ArrayList<String>();

            for (Topic topic : topics) {
                topicUuids.add(topic.getUuid());
            }

            if (!topics.isEmpty()) {
                Map<String, List<Poster>> topicPosters = storage.getPostersForTopics(topicUuids);
                Map<String, Long> postCounts = storage.getPostCountsForTopics(topicUuids);
//                Map<String, Long> lastActivityTimes = storage.getLastActivityTimeForTopics(topicUuids);

                for (Topic topic : topics) {
                    if (topicPosters.containsKey(topic.getUuid())) {
                        topic.setPosters(topicPosters.get(topic.getUuid()));
                    }
                    if (postCounts.containsKey(topic.getUuid())) {
                        topic.setPostCount(postCounts.get(topic.getUuid()));
                    }
//                    if (lastActivityTimes.containsKey(topic.getUuid())) {
//                        topic.setLastActivityTime(lastActivityTimes.get(topic.getUuid()));
//                    }
                }
            }

            JSONObject result = new JSONObject();
            result.put("count", topicsCount);
            result.put("page", page);
            result.put("pageSize", PAGE_SIZE);

            JSONArray topicsJSON = new JSONArray();
            for (Topic topic: topics) {
                topicsJSON.add(topic.asJSONObject());
            }
            result.put("topics", topicsJSON);

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
}