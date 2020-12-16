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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

import org.sakaiproject.conversations.tool.storage.FileStore;

import org.apache.commons.fileupload.FileItem;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.io.Writer;
import java.util.UUID;
import java.io.OutputStream;

public class FileHandler implements Handler {

    static long SENSIBLE_ATTACHMENT_SIZE_BYTES = 100 * 1024 * 1024;
    static long SENSIBLE_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

    private String redirectTo = null;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        try {
            final String mode = request.getParameter("mode");

            if ("inline-upload".equals(mode)) {
                handleUpload(request, response, context);
            } else if ("attachment".equals(mode)) {
                handleAttachmentUpload(request, response, context);
            } else if ("view".equals(mode)) {
                handleView(request, response, context);
            } else {
                throw new RuntimeException("Mode must be one of: inline-upload or view");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleUpload(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws Exception {
        FileItem uploadedFile = (FileItem)request.getAttribute("file");

        String imageType = "";

        switch (uploadedFile.getContentType()) {
        case "image/jpeg":
            imageType = "jpeg";
            break;
        case "image/png":
            imageType = "png";
            break;
        default:
            throw new RuntimeException("Unsupported image type");
        }

        if (uploadedFile.getSize() > SENSIBLE_IMAGE_SIZE_BYTES) {
            throw new RuntimeException("Image too large");
        }

        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream((int)uploadedFile.getSize());
        InputStream stream = uploadedFile.getInputStream();
        byte[] buf = new byte[4096];
        int len = 0;

        while ((len = stream.read(buf)) > 0) {
            imageBytes.write(buf, 0, len);

            if (imageBytes.size() > SENSIBLE_IMAGE_SIZE_BYTES) {
                // Paranoia
                throw new RuntimeException("Image too large");
            }
        }

        byte[] resized = maybeResize(imageBytes.toByteArray(), 600, 600, imageType);

        String key = new FileStore().storeInline(resized, uploadedFile.getName(), uploadedFile.getContentType());

        response.setHeader("Content-type", "text/json");
        response.getWriter().write(String.format("{\"key\": \"%s\"}", key));
    }

    public void handleAttachmentUpload(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws Exception {
        FileItem uploadedFile = (FileItem)request.getAttribute("file");

        if (uploadedFile.getSize() > SENSIBLE_ATTACHMENT_SIZE_BYTES) {
            throw new RuntimeException("Attachment too large");
        }

        String key = new FileStore().storeAttachment(uploadedFile.getInputStream(),
                                                     uploadedFile.getName(),
                                                     uploadedFile.getContentType());

        response.setHeader("Content-type", "text/json");
        response.getWriter().write(String.format("{\"key\": \"%s\"}", key));
    }

    public void handleView(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws Exception {
        String key = request.getParameter("key");

        // sanity check.  FIXME: Better handling here.
        UUID.fromString(key);

        Optional<FileStore.Handle> maybeHandle = new FileStore().read(key);

        if (maybeHandle.isPresent()) {
            FileStore.Handle f = maybeHandle.get();

            response.setHeader("Content-type", f.mimeType);

            if (f.attachment)  {
                response.setHeader("Content-disposition", "attachment; filename=" + f.fileName);
            }

            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[4096];
            int len;

            try {
                while ((len = f.inputStream.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                f.inputStream.close();
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private byte[] maybeResize(byte[] imageBytes, int maxWidth, int maxHeight, String targetImageType) throws Exception {
        BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

        int originalWidth = sourceImage.getWidth();
        int originalHeight = sourceImage.getHeight();

        if (originalWidth < maxWidth && originalHeight < maxHeight) {
            // Fine as it is
            return imageBytes;
        }

        int newWidth;
        int newHeight;

        if (originalWidth > originalHeight) {
            float aspect = originalWidth / (float)maxWidth;
            newWidth = maxWidth;
            newHeight = (int)Math.floor(originalHeight / aspect);
        } else {
            float aspect = originalHeight / (float)maxHeight;
            newHeight = maxHeight;
            newWidth = (int)Math.floor(originalWidth / aspect);
        }

        java.awt.Image scaledImage = sourceImage.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);

        BufferedImage result = new BufferedImage(newWidth,
                                                 newHeight,
                                                 sourceImage.getType());

        Graphics g = result.createGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ImageIO.write(result, targetImageType, byteOutput);

        return byteOutput.toByteArray();
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

    public boolean hasTemplate() {
        return false;
    }

    public Map<String, List<String>> getFlashMessages() {
        return new HashMap<String, List<String>>();
    }
}
