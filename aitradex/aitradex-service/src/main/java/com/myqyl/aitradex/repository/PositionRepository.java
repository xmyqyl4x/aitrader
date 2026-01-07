package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.Position;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, UUID> {
}
