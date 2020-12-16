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

package org.sakaiproject.conversations.tool;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sakaiproject.conversations.tool.handlers.*;

import org.sakaiproject.conversations.tool.storage.ConversationsStorage;

public class ConversationsServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationsServlet.class);

    public static final String ROLE_INSTRUCTOR = "instructor";
    public static final String ROLE_STUDENT = "student";

    public void init(ServletConfig config) throws ServletException {
        if (ServerConfigurationService.getBoolean("auto.ddl", false) || ServerConfigurationService.getBoolean("auto.ddl.conversations", false)) {
            new ConversationsStorage().runDBMigrations();
        }

        super.init(config);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // What's the plan here?
        // Need to get a token for the current user (auth flow if they've not yet done that) {
        // Then fetch a list of their resources {
        // And stick them on the page...

        I18n i18n = new I18n(this.getClass().getClassLoader(), "org.sakaiproject.conversations.tool.i18n.conversations");

        URL toolBaseURL = determineBaseURL();
        Handlebars handlebars = loadHandlebars(toolBaseURL, i18n);

        try {
            Map<String, Object> context = new HashMap<String, Object>();

            context.put("baseURL", toolBaseURL);
            context.put("layout", true);
            context.put("skinRepo", ServerConfigurationService.getString("skin.repo", ""));
            context.put("randomSakaiHeadStuff", request.getAttribute("sakai.html.head"));
            context.put("siteId", ToolManager.getCurrentPlacement().getContext());

            User currentUser = UserDirectoryService.getCurrentUser();
            context.put("currentUserId", currentUser.getId());
            String role = ROLE_STUDENT;
            if (SecurityService.unlock("site.upd", SiteService.siteReference(ToolManager.getCurrentPlacement().getContext()))) {
                role = ROLE_INSTRUCTOR;
            }
            context.put("currentUserRole", role);
            context.put("isInstructor", ROLE_INSTRUCTOR.equals(role));
            context.put("isStudent", ROLE_STUDENT.equals(role));

            Handler handler = handlerForRequest(request);

            if (!handler.isRolePermitted(role)) {
                response.setStatus(403);
                response.getWriter().write("Insufficient privileges");
                return;
            }

            handler.handle(request, response, context);

            if (!response.containsHeader("Content-Type")) {
                response.setHeader("Content-Type", handler.getContentType());
            }

            if (handler.hasRedirect()) {
                if (handler.getRedirect().startsWith("http")) {
                    response.sendRedirect(handler.getRedirect());
                } else {
                    response.sendRedirect(toolBaseURL + handler.getRedirect());
                }
            } else if (handler.hasTemplate()) {
                if (Boolean.TRUE.equals(context.get("layout"))) {
                    Template template = handlebars.compile("org/sakaiproject/conversations/tool/views/layout");
                    response.getWriter().write(template.apply(context));
                } else {
                    Template template = handlebars.compile("org/sakaiproject/conversations/tool/views/" + context.get("subpage"));
                    response.getWriter().write(template.apply(context));
                }
            }
        } catch (IOException e) {
            LOG.warn("Write failed", e);
        }
    }

    private Handler handlerForRequest(HttpServletRequest request) {
        String path = request.getPathInfo();

        if (path == null) {
            path = "";
        }

        if (path.equals("/new-topic")) {
            return new NewTopicHandler();
        } else if (path.equals("/create-topic")) {
            return new CreateTopicHandler();
        } else if (path.equals("/update-topic")) {
            return new UpdateTopicSettingsHandler();
        } else if (path.equals("/topic")) {
            return new TopicHandler();
        } else if (path.equals("/feed/posts")) {
            return new PostsFeedHandler();
        } else if (path.equals("/feed/topic")) {
            return new TopicFeedHandler();
        } else if (path.equals("/feed/topics")) {
            return new TopicsFeedHandler();
        } else if (path.equals("/create-post")) {
            return new CreatePostHandler();
        } else if (path.equals("/update-post")) {
            return new UpdatePostHandler();
        } else if (path.equals("/mark-topic-read")) {
            return new TopicReadEventHandler();
        } else if (path.equals("/file-upload")) {
            return new FileHandler();
        } else if (path.equals("/file-view")) {
            return new FileHandler();
        } else if (path.equals("/like-post")) {
            return new LikePostHandler();
        }

        return new IndexHandler();
    }

    private URL determineBaseURL() {
        try {
            return new URL(ServerConfigurationService.getPortalUrl() + getBaseURI() + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Couldn't determine tool URL", e);
        }
    }

    private String getBaseURI() {
        String result = "";

        String siteId = null;
        String toolId = null;

        if (ToolManager.getCurrentPlacement() != null) {
            siteId = ToolManager.getCurrentPlacement().getContext();
            toolId = ToolManager.getCurrentPlacement().getId();
        }

        if (siteId != null) {
            result += "/site/" + siteId;
            if (toolId != null) {
                result += "/tool/" + toolId;
            }
        }

        return result;
    }

    private Handlebars loadHandlebars(final URL baseURL, final I18n i18n) {
        Handlebars handlebars = new Handlebars();

        handlebars.setInfiniteLoops(true);

        handlebars.registerHelper("subpage", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String subpage = options.param(0);
                try {
                    Template template = handlebars.compile("org/sakaiproject/conversations/tool/views/" + subpage);
                    return template.apply(context);
                } catch (IOException e) {
                    LOG.warn("IOException while loading subpage", e);
                    return "";
                }
            }
        });

        handlebars.registerHelper(Handlebars.HELPER_MISSING, new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) throws IOException {
                throw new RuntimeException("Failed to find a match for: " + options.fn.text());
            }
        });

        handlebars.registerHelper("show-time", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                long utcTime = options.param(0) == null ? 0 : options.param(0);

                if (utcTime == 0) {
                    return "-";
                }

                Time time = TimeService.newTime(utcTime);

                return time.toStringLocalFull();
            }
        });

        handlebars.registerHelper("actionURL", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String type = options.param(0);
                String uuid = options.param(1);
                String action = options.param(2);

                try {
                    return new URL(baseURL, type + "/" + uuid + "/" + action).toString();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed while building action URL", e);
                }
            }
        });

        handlebars.registerHelper("newURL", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String type = options.param(0);
                String action = options.param(1);

                try {
                    return new URL(baseURL, type + "/" + action).toString();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed while building newURL", e);
                }
            }
        });

        handlebars.registerHelper("t", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String key = Arrays.stream(options.params).map(Object::toString).collect(Collectors.joining("_"));
                return i18n.t(key);
            }
        });

        handlebars.registerHelper("escape", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String escapeMe = options.param(0);
                return new Handlebars.SafeString(escapeMe);
            }
        });

        handlebars.registerHelper("selected", new Helper<Object>() {
            @Override
            public CharSequence apply(final Object context, final Options options) {
                String option = options.param(0);
                String value = options.param(1);

                return option.equals(value) ? "selected" : "";
            }
        });

        return handlebars;
    }
}
