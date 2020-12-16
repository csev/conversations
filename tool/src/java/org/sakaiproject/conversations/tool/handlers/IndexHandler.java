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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;

public class IndexHandler implements Handler {

    private String redirectTo = null;

    public IndexHandler() {
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            context.put("page", 0);
            context.put("pageSize", TopicsFeedHandler.PAGE_SIZE);
            context.put("order_by", "last_activity_at");
            context.put("order_direction", "desc");
            context.put("subpage", "index");

            if ((Boolean)context.get("isInstructor")) {
                String siteId = (String)context.get("siteId");
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

                context.put("siteGroupsJSON", groupsJSON.toJSONString());
            }

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
}