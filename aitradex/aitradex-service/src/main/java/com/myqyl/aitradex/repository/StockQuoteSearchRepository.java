package com.myqyl.aitradex.repository;

import com.myqyl.aitradex.domain.StockQuoteSearch;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockQuoteSearchRepository extends JpaRepository<StockQuoteSearch, UUID> {

  Page<StockQuoteSearch> findByOrderByCreatedAtDesc(Pageable pageable);

  Page<StockQuoteSearch> findBySymbolIgnoreCaseOrderByCreatedAtDesc(
      String symbol, Pageable pageable);

  Page<StockQuoteSearch> findByStatusOrderByCreatedAtDesc(
      StockQuoteSearch.SearchStatus status, Pageable pageable);

  @Query(
      "SELECT s FROM StockQuoteSearch s WHERE "
          + "(:symbol IS NULL OR UPPER(s.symbol) = UPPER(:symbol)) "
          + "AND (:status IS NULL OR s.status = :status) "
          + "AND (:dateFrom IS NULL OR s.createdAt >= :dateFrom) "
          + "AND (:dateTo IS NULL OR s.createdAt <= :dateTo) "
          + "ORDER BY s.createdAt DESC")
  Page<StockQuoteSearch> findByFilters(
      @Param("symbol") String symbol,
      @Param("status") StockQuoteSearch.SearchStatus status,
      @Param("dateFrom") java.time.OffsetDateTime dateFrom,
      @Param("dateTo") java.time.OffsetDateTime dateTo,
      Pageable pageable);

  List<StockQuoteSearch> findTop100ByOrderByCreatedAtDesc();
}
