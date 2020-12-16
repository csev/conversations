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

import org.sakaiproject.conversations.tool.models.*;
import org.sakaiproject.conversations.tool.storage.ConversationsStorage;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

public class PostsFeedHandler implements Handler {

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            String topicUuid = p.getString("topicUuid", null);

            if (topicUuid == null) {
                // FIXME
                throw new RuntimeException("topicUuid required");
            }

            User currentUser = UserDirectoryService.getCurrentUser();
            String siteId = (String) context.get("siteId");

            ConversationsStorage storage = new ConversationsStorage();


            Optional<Topic> topic = storage.getTopic(topicUuid, siteId);

            if (!topic.isPresent()) {
                throw new RuntimeException("topicUuid does not exist");
            }

            List<Post> posts = storage.getPosts(topicUuid);
            Long timeLastVisited = storage.getLastReadTopic(topicUuid, currentUser.getId());

            Collections.sort(posts);

            if ((Boolean) context.get("isStudent")) {
                if (topic.get().getSettings().isRequirePost()) {
                    if (posts.stream().noneMatch(t -> currentUser.getId().equals(t.getPostedBy()))) {
                        posts = Arrays.asList(posts.get(0));
                    }
                }
            }

            Map<String, Post> topLevelPosts = new HashMap<String, Post>();

            for (Post post : posts) {
                if (post.getPostedBy().equals(currentUser.getId())) {
                    post.setEditable(true);
                    post.setLikeable(false);
                } else {
                    post.setUnread(post.getPostedAt() > timeLastVisited);
                    post.setLikeable(true);
                }

                if (post.getParentPostUuid() == null) {
                    topLevelPosts.put(post.getUuid(), post);
                } else {
                    topLevelPosts.get(post.getParentPostUuid()).addComment(post);
                }

                if (post.getLikedBy().contains(currentUser.getId())) {
                    post.setLiked(true);
                }
            }

            JSONArray result = new JSONArray();


            for (Post post : posts) {
                if (topLevelPosts.containsKey(post.getUuid())) {
                    result.add(postAsJSON(post));
                }
            }

            response.getWriter().write(result.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JSONObject postAsJSON(Post post) throws MissingUuidException {
        JSONObject obj = new JSONObject();

        obj.put("uuid", post.getUuid());
        obj.put("content", post.getContent());
        obj.put("postedBy", post.getPostedBy());
        obj.put("postedByDisplayName", post.getPostedByDisplayName());
        obj.put("postedAt", post.getPostedAt());
        obj.put("unread", post.isUnread());
        obj.put("editable", post.isEditable());
        obj.put("version", post.getVersion());
        obj.put("liked", post.isLiked());
        obj.put("likes", post.getLikedBy().size());
        obj.put("likeable", post.isLikeable());

        JSONArray comments = new JSONArray();
        for (Post comment : post.getComments()) {
            comments.add(postAsJSON(comment));
        }
        obj.put("comments", comments);

        JSONArray attachments = new JSONArray();
        for (Attachment attachment : post.getAttachments()) {
            attachments.add(attachment.asJSONObject());
        }
        obj.put("attachments", attachments);

        return obj;
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
