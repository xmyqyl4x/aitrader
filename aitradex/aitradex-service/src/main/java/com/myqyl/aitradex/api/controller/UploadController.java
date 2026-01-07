package com.myqyl.aitradex.api.controller;

import com.myqyl.aitradex.api.dto.CreateUploadRequest;
import com.myqyl.aitradex.api.dto.UpdateUploadStatusRequest;
import com.myqyl.aitradex.api.dto.UploadDto;
import com.myqyl.aitradex.domain.UploadType;
import com.myqyl.aitradex.service.UploadService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

  private final UploadService uploadService;

  public UploadController(UploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UploadDto create(@Valid @RequestBody CreateUploadRequest request) {
    return uploadService.create(request);
  }

  @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UploadDto uploadFile(
      @RequestParam("userId") UUID userId,
      @RequestParam("type") UploadType type,
      @RequestParam("file") MultipartFile file) {
    return uploadService.storeFile(userId, type, file);
  }

  @GetMapping
  public List<UploadDto> list() {
    return uploadService.list();
  }

  @GetMapping("/{id}")
  public UploadDto get(@PathVariable UUID id) {
    return uploadService.get(id);
  }

  @PatchMapping("/{id}/status")
  public UploadDto updateStatus(
      @PathVariable UUID id, @Valid @RequestBody UpdateUploadStatusRequest request) {
    return uploadService.updateStatus(id, request);
  }
}
