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

package org.sakaiproject.conversations.tool.storage;

import org.sakaiproject.component.cover.ServerConfigurationService;
import java.nio.file.Paths;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.conversations.tool.models.Attachment;

@Slf4j
public class FileStore {

    public class Handle {
        public InputStream inputStream;
        public String mimeType;
        public String fileName;
        public boolean attachment;
    }

    public String storeInline(byte[] content, String filename, String mimeType) throws Exception {
        return store(new ByteArrayInputStream(content), filename, mimeType, "inline");
    }

    public String storeAttachment(InputStream content, String filename, String mimeType) throws Exception {
        return store(content, filename, mimeType, "attachment");
    }

    private String store(InputStream content, String filename, String mimeType, String role) throws Exception {
        String basedir = ServerConfigurationService.getString("conversation-tool.storage", "/tmp/conversation");

        String key = UUID.randomUUID().toString();

        String subdir = key.split("-")[0];

        Paths.get(basedir, subdir).toFile().mkdirs();

        File output = Paths.get(basedir, subdir, key).toFile();

        byte[] buf = new byte[4096];
        int len;

        try (FileOutputStream out = new FileOutputStream(output)) {
            while ((len = content.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }

        storeMetadata(key, mimeType, filename, role);

        return key;
    }

    private void storeMetadata(String key, String mimeType, String fileName, String role) {
        new ConversationsStorage().storeFileMetadata(key, mimeType, fileName, role);
    }

    public Optional<Handle> read(String key) throws Exception {
        String basedir = ServerConfigurationService.getString("conversation-tool.storage", "/tmp/conversation");
        String subdir = key.split("-")[0];

        File dataFile = Paths.get(basedir, subdir, key).toFile();

        Handle result = new Handle();

        Optional<Attachment> attachment = new ConversationsStorage().readFileMetadata(key);

        if (attachment.isPresent()) {
            result.mimeType = attachment.get().mimeType;
            result.fileName = attachment.get().fileName;
            result.attachment = "attachment".equals(attachment.get().role);
            result.inputStream = new FileInputStream(dataFile);

            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }
}
