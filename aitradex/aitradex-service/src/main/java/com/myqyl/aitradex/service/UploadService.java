package com.myqyl.aitradex.service;

import com.myqyl.aitradex.api.dto.CreateUploadRequest;
import com.myqyl.aitradex.api.dto.UpdateUploadStatusRequest;
import com.myqyl.aitradex.api.dto.UploadDto;
import com.myqyl.aitradex.domain.Upload;
import com.myqyl.aitradex.domain.UploadStatus;
import com.myqyl.aitradex.domain.User;
import com.myqyl.aitradex.exception.NotFoundException;
import com.myqyl.aitradex.repository.UploadRepository;
import com.myqyl.aitradex.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadService {

  private final UploadRepository uploadRepository;
  private final UserRepository userRepository;

  public UploadService(UploadRepository uploadRepository, UserRepository userRepository) {
    this.uploadRepository = uploadRepository;
    this.userRepository = userRepository;
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

  @Transactional(readOnly = true)
  public List<UploadDto> list() {
    return uploadRepository.findAll().stream().map(this::toDto).toList();
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
}
