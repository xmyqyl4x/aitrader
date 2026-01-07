package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.QuoteSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteSnapshotRepository extends JpaRepository<QuoteSnapshot, UUID> {

  List<QuoteSnapshot> findBySymbolOrderByAsOfDesc(String symbol);

  List<QuoteSnapshot> findTop100ByOrderByAsOfDesc();
}
