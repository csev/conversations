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

import org.json.simple.JSONObject;

public class Attachment {
    public String key;
    public String mimeType;
    public String fileName;
    public String role;


    public Attachment(String key, String mimeType, String fileName, String role) {
        this.key = key;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.role = role;
    }

    public JSONObject asJSONObject() {
        JSONObject obj = new JSONObject();

        obj.put("key", this.key);
        obj.put("mimeType", this.mimeType);
        obj.put("fileName", this.fileName);
        obj.put("role", this.role);

        return obj;
    }
}
