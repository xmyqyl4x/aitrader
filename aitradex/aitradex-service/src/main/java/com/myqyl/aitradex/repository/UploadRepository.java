package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.Upload;
import com.myqyl.aitradex.domain.UploadStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadRepository extends JpaRepository<Upload, UUID> {
  List<Upload> findByUserIdOrderByCreatedAtDesc(UUID userId);

  List<Upload> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, UploadStatus status);

  List<Upload> findByStatusOrderByCreatedAtDesc(UploadStatus status);
}
