package org.sakaiproject.conversations.tool.handlers;

import org.json.simple.JSONObject;
import org.sakaiproject.conversations.tool.models.Post;
import org.sakaiproject.conversations.tool.storage.ConversationsStorage;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicReadEventHandler implements Handler {
    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            RequestParams p = new RequestParams(request);

            String topicUuid = p.getString("topicUuid", null);

            if (topicUuid == null) {
                throw new RuntimeException("topicUuid required");
            }

            User currentUser = UserDirectoryService.getCurrentUser();

            String eventUuid = new ConversationsStorage().setLastReadTopicEvent(topicUuid, currentUser.getId());

            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("uuid", eventUuid);

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
