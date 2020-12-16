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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Poster {
    @Getter
    private final String userId;
    @Getter
    private final String netId;
    @Getter
    private final String firstName;
    @Getter
    private final String lastName;
    @Getter
    private final Long latestPostAt;

    public Poster(String userId, String netId, String firstName, String lastName, Long latestPostAt) {
        this.userId = userId;
        this.netId = netId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.latestPostAt = latestPostAt;
    }

    public JSONObject asJSONObject() {
        JSONObject obj = new JSONObject();

        obj.put("userId", this.userId);
        obj.put("netId", this.netId);
        obj.put("firstName", this.firstName);
        obj.put("lastName", this.lastName);
        obj.put("latestPostAt", this.latestPostAt);

        return obj;
    }
}
