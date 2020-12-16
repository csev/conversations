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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class Post implements Comparable<Post> {
    private final String uuid;
    @Getter
    @Setter
    private String content;
    @Getter
    private final String postedBy;
    @Getter
    private final String postedByEid;
    @Getter
    private final Long postedAt;
    @Setter
    @Getter
    private String updatedBy;
    @Setter
    @Getter
    private Long updatedAt;
    @Getter
    @Setter
    private Long version;
    @Getter
    private final String parentPostUuid;
    @Getter
    private final String postedByDisplayName;
    @Getter
    private final List<Attachment> attachments;
    @Getter
    private final List<Post> comments;
    @Setter
    @Getter
    private boolean unread = false;
    @Setter
    @Getter
    private boolean editable = false;
    @Setter
    @Getter
    private List<String> likedBy = new ArrayList<String>();
    @Setter
    @Getter
    private boolean liked = false;
    @Setter
    @Getter
    private boolean likeable = false;

    public Post(String uuid, String content, String postedBy, Long postedAt, String parentPostUuid, String postedByEid, String postedByFirstName, String postedByLastName, Long version) {
        this.uuid = uuid;
        this.content = content;
        this.postedBy = postedBy;
        this.postedAt = postedAt;
        this.postedByEid = postedByEid;
        this.parentPostUuid = parentPostUuid;
        this.attachments = new ArrayList<Attachment>();
        this.comments = new ArrayList<Post>();
        if (postedByFirstName != null && postedByLastName != null) {
            this.postedByDisplayName = postedByFirstName + " " + postedByLastName;
        } else {
            this.postedByDisplayName = postedByEid;
        }
        this.version = version;
    }

    public Post(String content, String postedBy) {
        this.uuid = null;
        this.content = content;
        this.postedBy = postedBy;
        this.postedByEid = null;
        this.postedAt = null;
        this.parentPostUuid = null;
        this.attachments = new ArrayList<Attachment>();
        this.comments = new ArrayList<Post>();
        this.postedByDisplayName = null;
    }

    public void addComment(Post comment) {
        comments.add(comment);
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
    }

    @Override
    public int compareTo(Post other) {
        // React: sort oldest first
        return getPostedAt().compareTo(other.getPostedAt());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Post)) {
            return false;
        }

        try {
            return uuid.equals(((Post)obj).getUuid());
        } catch (MissingUuidException e) {
            return false;
        }
    }

    public int hashCode() {
        return uuid.hashCode();
    }

    public String getUuid() throws MissingUuidException {
        if (this.uuid == null) {
            throw new MissingUuidException("No UUID has been set for this topic");
        }

        return this.uuid;
    }

    public Errors validate() {
        Errors errors = new Errors();

        return errors;
    }
}
