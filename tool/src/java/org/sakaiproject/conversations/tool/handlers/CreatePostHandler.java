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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.sakaiproject.conversations.tool.ConversationsServlet;
import org.sakaiproject.conversations.tool.lib.HTMLSanitizer;
import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.models.Topic;
import org.sakaiproject.conversations.tool.storage.ConversationsStorage;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

public class CreatePostHandler implements Handler {

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            String topicUuid = p.getString("topicUuid", null);
            String content = p.getString("content", null);
            String parentPostUuid = p.getString("post_uuid", null);

            if (content != null) {
                System.err.println("BEFORE SANITIZE: " + content);
                content = HTMLSanitizer.sanitize(content);
                System.err.println("AFTER SANITIZE: " + content);

                if ("".equals(content)) {
                    // Null out and throw an exception in a mo
                    content = null;
                }
            }

            if (topicUuid == null || content == null) {
                // FIXME
                throw new RuntimeException("topicUuid and content required");
            }

            List<String> attachmentKeys = Collections.emptyList();
            if (request.getParameterValues("attachmentKeys[]") != null) {
                attachmentKeys = Arrays.asList(request.getParameterValues("attachmentKeys[]"));
            }

            User currentUser = UserDirectoryService.getCurrentUser();
            Post post = new Post(content, currentUser.getId());

            String postUuid = new ConversationsStorage().createPost(post, topicUuid, parentPostUuid, attachmentKeys, System.currentTimeMillis());

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("uuid", postUuid);
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
