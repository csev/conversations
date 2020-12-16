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

import org.json.simple.JSONObject;
import org.sakaiproject.conversations.tool.lib.HTMLSanitizer;
import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.storage.ConversationsStorage;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class UpdatePostHandler implements Handler {

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            String topicUuid = p.getString("topic_uuid", null);
            String content = p.getString("content", null);
            String postUuid = p.getString("post_uuid", null);
            String version = p.getString("version", null);

            if (content != null) {
                System.err.println("BEFORE SANITIZE: " + content);
                content = HTMLSanitizer.sanitize(content);
                System.err.println("AFTER SANITIZE: " + content);

                if ("".equals(content)) {
                    // Null out and throw an exception in a mo
                    content = null;
                }
            }

            if (topicUuid == null || postUuid == null || content == null || version == null) {
                // FIXME
                throw new RuntimeException("topic_uuid, post_uuid, version, content required");
            }

            List<String> attachmentKeys = Collections.emptyList();
            if (request.getParameterValues("attachmentKeys[]") != null) {
                attachmentKeys = Arrays.asList(request.getParameterValues("attachmentKeys[]"));
            }

            ConversationsStorage storage = new ConversationsStorage();
            Post post = storage.getPost(postUuid);

            if (!post.getPostedBy().equals(context.get("currentUserId"))) {
                // FIXME
                throw new RuntimeException("Unable to edit post");
            }

            if (!post.getVersion().equals(Long.valueOf(version))) {
                // FIXME
                throw new RuntimeException("Unable to edit as post is stale");
            }

            post.setContent(content);
            post.setUpdatedBy((String) context.get("currentUserId"));
            post.setVersion(post.getVersion() + 1);

            storage.updatePost(post, topicUuid, attachmentKeys, System.currentTimeMillis());

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
