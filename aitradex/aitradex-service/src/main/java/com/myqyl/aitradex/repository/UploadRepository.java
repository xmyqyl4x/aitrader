package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.Upload;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadRepository extends JpaRepository<Upload, UUID> {
}
