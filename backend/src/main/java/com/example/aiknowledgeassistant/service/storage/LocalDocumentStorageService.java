package com.example.aiknowledgeassistant.service.storage;

import com.example.aiknowledgeassistant.exception.BadRequestException;
import com.example.aiknowledgeassistant.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalDocumentStorageService implements DocumentStorageService {

    private final Path uploadLocation;

    public LocalDocumentStorageService(@Value("${cognivault.document.upload-dir:./uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.uploadLocation);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize storage location: " + this.uploadLocation, e);
        }
    }

    @Override
    public String store(MultipartFile file, String storedFileName) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file.");
        }
        try {
            Path targetLocation = this.uploadLocation.resolve(storedFileName).normalize();
            if (!targetLocation.startsWith(this.uploadLocation)) {
                throw new BadRequestException("Cannot store file outside current directory.");
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetLocation.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file " + storedFileName, e);
        }
    }

    @Override
    public InputStream loadAsStream(String storagePath) {
        try {
            File file = new File(storagePath);
            if (!file.exists()) {
                throw new ResourceNotFoundException("File not found at path: " + storagePath);
            }
            return new FileInputStream(file);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Could not read file at path: " + storagePath);
        }
    }

    @Override
    public Resource loadAsResource(String storagePath) {
        File file = new File(storagePath);
        if (!file.exists()) {
            throw new ResourceNotFoundException("File not found at path: " + storagePath);
        }
        return new FileSystemResource(file);
    }

    @Override
    public boolean delete(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            return Files.deleteIfExists(path);
        } catch (Exception e) {
            return false;
        }
    }
}
