package com.myqyl.aitradex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myqyl.aitradex.api.dto.UploadDto;
import com.myqyl.aitradex.domain.Upload;
import com.myqyl.aitradex.domain.UploadStatus;
import com.myqyl.aitradex.domain.UploadType;
import com.myqyl.aitradex.domain.User;
import com.myqyl.aitradex.repository.UploadRepository;
import com.myqyl.aitradex.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

  @Mock private UploadRepository uploadRepository;
  @Mock private UserRepository userRepository;

  @TempDir Path tempDir;

  @Test
  void validateAndStageCapturesCsvErrors() throws Exception {
    Path csv = tempDir.resolve("bad.csv");
    Files.writeString(csv, "symbol,qty\nAAPL\n");
    Upload upload =
        Upload.builder()
            .id(UUID.randomUUID())
            .user(User.builder().id(UUID.randomUUID()).build())
            .type(UploadType.CSV)
            .status(UploadStatus.PROCESSING)
            .fileName("bad.csv")
            .storedPath(csv.toString())
            .build();

    when(uploadRepository.findById(upload.getId())).thenReturn(Optional.of(upload));
    when(uploadRepository.save(any(Upload.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UploadService service =
        new UploadService(
            uploadRepository,
            userRepository,
            tempDir.toString(),
            25,
            50,
            new ObjectMapper());

    UploadDto result = service.validateAndStage(upload.getId());

    assertEquals(UploadStatus.FAILED, result.status());
    assertTrue(result.errorReport().contains("Column count mismatch"));
  }
}
