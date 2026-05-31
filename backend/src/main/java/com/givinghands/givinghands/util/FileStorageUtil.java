package com.givinghands.givinghands.util;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileStorageUtil {

    private FileStorageUtil() {}

    public static String storeMultipartFile(Path targetDir, String originalFilename, byte[] bytes) throws IOException {
        if (originalFilename == null) originalFilename = "file";
        String ext = "";
        String name = originalFilename;
        int idx = originalFilename.lastIndexOf('.');
        if (idx > -1 && idx < originalFilename.length() - 1) {
            ext = originalFilename.substring(idx);
            name = originalFilename.substring(0, idx);
        }
        String safeBase = StringUtils.hasText(name) ? name.replaceAll("[^a-zA-Z0-9_-]", "") : "file";
        String finalFilename = safeBase + "-" + UUID.randomUUID() + ext;

        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(finalFilename);
        Files.write(targetFile, bytes);

        return finalFilename;
    }

    public static String storeMultipartFile(Path targetDir, String originalFilename, org.springframework.web.multipart.MultipartFile file) throws IOException {
        if (file == null) throw new IllegalArgumentException("file cannot be null");
        String finalFilename = originalFilename;
        if (originalFilename == null) originalFilename = "file";
        int idx = originalFilename.lastIndexOf('.');
        String ext = idx > -1 ? originalFilename.substring(idx) : "";
        String safeBase = originalFilename.substring(0, idx > -1 ? idx : originalFilename.length())
                .replaceAll("[^a-zA-Z0-9_-]", "");
        if (!StringUtils.hasText(safeBase)) safeBase = "file";
        finalFilename = safeBase + "-" + UUID.randomUUID() + ext;

        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(finalFilename);
        Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        return finalFilename;
    }
}

