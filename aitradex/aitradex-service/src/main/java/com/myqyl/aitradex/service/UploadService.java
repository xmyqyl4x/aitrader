package com.myqyl.aitradex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.CreateUploadRequest;
import com.myqyl.aitradex.api.dto.UpdateUploadStatusRequest;
import com.myqyl.aitradex.api.dto.UploadDto;
import com.myqyl.aitradex.domain.Upload;
import com.myqyl.aitradex.domain.UploadStatus;
import com.myqyl.aitradex.domain.UploadType;
import com.myqyl.aitradex.domain.User;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.UploadRepository;
import com.myqyl.aitradex.repository.UserRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

  private final UploadRepository uploadRepository;
  private final UserRepository userRepository;
  private final Path uploadDirectory;
  private final long maxUploadBytes;
  private final int maxValidationErrors;
  private final ObjectMapper objectMapper;

  public UploadService(
      UploadRepository uploadRepository,
      UserRepository userRepository,
      @Value("${app.uploads.directory}") String uploadDirectory,
      @Value("${app.uploads.max-size-mb:25}") long maxUploadSizeMb,
      @Value("${app.uploads.max-validation-errors:50}") int maxValidationErrors,
      ObjectMapper objectMapper) {
    this.uploadRepository = uploadRepository;
    this.userRepository = userRepository;
    this.uploadDirectory = Path.of(uploadDirectory);
    this.maxUploadBytes = maxUploadSizeMb * 1024 * 1024;
    this.maxValidationErrors = maxValidationErrors;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public UploadDto create(CreateUploadRequest request) {
    User user =
        userRepository.findById(request.userId()).orElseThrow(() -> userNotFound(request.userId()));

    Upload upload =
        Upload.builder()
            .user(user)
            .type(request.type())
            .status(UploadStatus.PENDING)
            .fileName(request.fileName())
            .storedPath(request.storedPath())
            .build();

    return toDto(uploadRepository.save(upload));
  }

  @Transactional
  public UploadDto updateStatus(UUID id, UpdateUploadStatusRequest request) {
    Upload upload = uploadRepository.findById(id).orElseThrow(() -> uploadNotFound(id));
    upload.setStatus(request.status());
    upload.setParsedRowCount(request.parsedRowCount());
    upload.setErrorReport(request.errorReport());
    if (request.storedPath() != null) {
      upload.setStoredPath(request.storedPath());
    }

    if (request.status() == UploadStatus.COMPLETED || request.status() == UploadStatus.FAILED) {
      upload.setCompletedAt(OffsetDateTime.now());
    }

    return toDto(uploadRepository.save(upload));
  }

  @Transactional
  public UploadDto storeFile(UUID userId, UploadType type, MultipartFile file) {
    User user = userRepository.findById(userId).orElseThrow(() -> userNotFound(userId));
    validateFile(type, file);
    Upload upload =
        Upload.builder()
            .user(user)
            .type(type)
            .status(UploadStatus.PROCESSING)
            .fileName(file.getOriginalFilename())
            .build();
    upload = uploadRepository.save(upload);

    try {
      Files.createDirectories(uploadDirectory);
      String targetName = upload.getId() + "-" + safeFilename(file.getOriginalFilename());
      Path targetPath = uploadDirectory.resolve(targetName);
      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
      upload.setStoredPath(targetPath.toString());
      upload.setStatus(UploadStatus.PROCESSING);
    } catch (IOException ex) {
      upload.setStatus(UploadStatus.FAILED);
      upload.setErrorReport(ex.getMessage());
      upload.setCompletedAt(OffsetDateTime.now());
    }

    return toDto(uploadRepository.save(upload));
  }

  @Transactional
  public UploadDto validateAndStage(UUID id) {
    Upload upload = uploadRepository.findById(id).orElseThrow(() -> uploadNotFound(id));
    if (upload.getStoredPath() == null) {
      throw new IllegalStateException("Upload has no stored file to validate");
    }

    upload.setStatus(UploadStatus.PROCESSING);
    uploadRepository.save(upload);

    Path path = Path.of(upload.getStoredPath());
    List<Map<String, Object>> errors = new ArrayList<>();
    int parsedRows = 0;

    try {
      if (upload.getType() == UploadType.CSV) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
          String headerLine = reader.readLine();
          if (headerLine == null || headerLine.isBlank()) {
            errors.add(error(1, "CSV header row is missing"));
          } else {
            String[] headers = headerLine.split(",", -1);
            String line;
            int rowIndex = 1;
            while ((line = reader.readLine()) != null) {
              rowIndex++;
              if (line.isBlank()) {
                errors.add(error(rowIndex, "Row is empty"));
              } else {
                String[] values = line.split(",", -1);
                if (values.length != headers.length) {
                  errors.add(
                      error(
                          rowIndex,
                          "Column count mismatch (expected %d, got %d)"
                              .formatted(headers.length, values.length)));
                }
                for (int i = 0; i < values.length; i++) {
                  if (values[i].isBlank()) {
                    errors.add(error(rowIndex, "Column %d is blank".formatted(i + 1)));
                    break;
                  }
                }
              }
              if (!line.isBlank()) {
                parsedRows++;
              }
              if (errors.size() >= maxValidationErrors) {
                break;
              }
            }
          }
        }
      } else if (upload.getType() == UploadType.JSON) {
        var node = objectMapper.readTree(Files.readString(path));
        if (node.isArray()) {
          parsedRows = node.size();
        } else if (!node.isMissingNode() && !node.isNull()) {
          parsedRows = 1;
        }
      } else {
        errors.add(error(0, "Excel validation is not yet supported"));
      }
    } catch (IOException ex) {
      errors.add(error(0, "Failed to read upload file: " + ex.getMessage()));
    }

    upload.setParsedRowCount(parsedRows);
    upload.setErrorReport(serializeErrors(errors));
    if (errors.isEmpty()) {
      upload.setStatus(UploadStatus.COMPLETED);
    } else {
      upload.setStatus(UploadStatus.FAILED);
    }
    upload.setCompletedAt(OffsetDateTime.now());

    return toDto(uploadRepository.save(upload));
  }

  @Transactional(readOnly = true)
  public List<UploadDto> list(UUID userId, UploadStatus status) {
    List<Upload> uploads;
    if (userId != null && status != null) {
      uploads = uploadRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    } else if (userId != null) {
      uploads = uploadRepository.findByUserIdOrderByCreatedAtDesc(userId);
    } else if (status != null) {
      uploads = uploadRepository.findByStatusOrderByCreatedAtDesc(status);
    } else {
      uploads = uploadRepository.findAll();
    }
    return uploads.stream().map(this::toDto).toList();
  }

  @Transactional(readOnly = true)
  public UploadDto get(UUID id) {
    return uploadRepository.findById(id).map(this::toDto).orElseThrow(() -> uploadNotFound(id));
  }

  private UploadDto toDto(Upload upload) {
    return new UploadDto(
        upload.getId(),
        upload.getUser().getId(),
        upload.getType(),
        upload.getStatus(),
        upload.getFileName(),
        upload.getStoredPath(),
        upload.getParsedRowCount(),
        upload.getErrorReport(),
        upload.getCreatedAt(),
        upload.getCompletedAt());
  }

  private NotFoundException uploadNotFound(UUID id) {
    return new NotFoundException("Upload %s not found".formatted(id));
  }

  private NotFoundException userNotFound(UUID id) {
    return new NotFoundException("User %s not found".formatted(id));
  }

  private String safeFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return "upload.bin";
    }
    return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private Map<String, Object> error(int row, String message) {
    Map<String, Object> entry = new HashMap<>();
    entry.put("row", row);
    entry.put("message", message);
    return entry;
  }

  private String serializeErrors(List<Map<String, Object>> errors) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("errors", errors);
    payload.put("errorCount", errors.size());
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      return "{\"errors\":[],\"errorCount\":0}";
    }
  }

  private void validateFile(UploadType type, MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("Uploaded file is empty");
    }
    if (file.getSize() > maxUploadBytes) {
      throw new IllegalArgumentException("Uploaded file exceeds size limit");
    }
    String contentType = file.getContentType();
    if (contentType != null && !contentType.isBlank()) {
      boolean validContentType =
          switch (type) {
            case CSV -> contentType.equals("text/csv")
                || contentType.equals("application/vnd.ms-excel");
            case JSON -> contentType.equals("application/json");
            case EXCEL -> contentType.equals("application/vnd.ms-excel")
                || contentType.equals(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
          };
      if (!validContentType) {
        throw new IllegalArgumentException("Uploaded file content type does not match type");
      }
    }
    String filename = file.getOriginalFilename();
    if (filename == null) {
      return;
    }
    String lower = filename.toLowerCase();
    boolean valid =
        switch (type) {
          case CSV -> lower.endsWith(".csv");
          case JSON -> lower.endsWith(".json");
          case EXCEL -> lower.endsWith(".xls") || lower.endsWith(".xlsx");
        };
    if (!valid) {
      throw new IllegalArgumentException("Uploaded file extension does not match type");
    }
  }
}
