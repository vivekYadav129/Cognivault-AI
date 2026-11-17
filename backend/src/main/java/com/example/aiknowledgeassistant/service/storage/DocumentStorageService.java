package com.example.aiknowledgeassistant.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DocumentStorageService {
    String store(MultipartFile file, String storedFileName);
    InputStream loadAsStream(String storagePath);
    Resource loadAsResource(String storagePath);
    boolean delete(String storagePath);
}
