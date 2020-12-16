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

package org.sakaiproject.conversations.tool.models;

import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicSettings {
    public static String AVAILABILITY_ENTIRE_SITE = "ENTIRE_SITE";
    public static String AVAILABILITY_GROUPS = "GROUPS";

    @Getter
    private final String topicUuid;
    @Setter
    @Getter
    private String availability = AVAILABILITY_ENTIRE_SITE;
    @Setter
    @Getter
    private boolean published = false;
    @Setter
    @Getter
    private boolean graded = false;
    @Setter
    @Getter
    private boolean allowComments = false;
    @Setter
    @Getter
    private boolean allowLike = false;
    @Setter
    @Getter
    private boolean requirePost = false;
    @Setter
    @Getter
    private List<String> groups = new ArrayList<String>();
    @Setter
    @Getter
    private Map<String,String> groupIdToName = new HashMap<>();

    public TopicSettings(String topicUuid, String availability, boolean published, boolean graded, boolean allowComments, boolean allowLike, boolean requirePost) {
        this.topicUuid = topicUuid;
        this.availability = availability;
        this.published = published;
        this.graded = graded;
        this.allowComments = allowComments;
        this.allowLike = allowLike;
        this.requirePost = requirePost;
    }

    public JSONObject asJSONObject() {
        JSONObject obj = new JSONObject();

        obj.put("topicUuid", this.topicUuid);
        obj.put("availability", this.availability);
        obj.put("published", this.published);
        obj.put("graded", this.graded);
        obj.put("allow_comments", this.allowComments);
        obj.put("allow_like", this.allowLike);
        obj.put("require_post", this.requirePost);
        JSONArray groupsJSON = new JSONArray();
        for (String groupRef : groups) {
            groupsJSON.add(groupRef);
        }
        obj.put("groups", groupsJSON);
        if (!this.groupIdToName.isEmpty()) {
            JSONObject groupNamesJSON = new JSONObject();
            for (String groupRef : this.groupIdToName.keySet()) {
                groupNamesJSON.put(groupRef, this.groupIdToName.get(groupRef));
            }
            obj.put("group_names", groupNamesJSON);
        }

        return obj;
    }
}
