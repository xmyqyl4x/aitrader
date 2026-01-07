package com.myqyl.aitradex.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myqyl.aitradex.api.dto.PortfolioSnapshotDto;
import com.myqyl.aitradex.config.SecurityConfig;
import com.myqyl.aitradex.security.JwtTokenService;
import com.myqyl.aitradex.service.PortfolioSnapshotService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PortfolioSnapshotController.class)
@Import(SecurityConfig.class)
class PortfolioSnapshotControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortfolioSnapshotService snapshotService;

  @MockBean private JwtTokenService jwtTokenService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void runSnapshotReturnsSnapshot() throws Exception {
    UUID accountId = UUID.randomUUID();
    PortfolioSnapshotDto dto =
        new PortfolioSnapshotDto(
            UUID.randomUUID(),
            accountId,
            LocalDate.of(2024, 1, 1),
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(500),
            BigDecimal.valueOf(10),
            BigDecimal.ZERO,
            OffsetDateTime.now());

    when(snapshotService.createSnapshotForAccount(accountId, null)).thenReturn(dto);

    mockMvc
        .perform(
            post("/api/portfolio-snapshots/run")
                .param("accountId", accountId.toString())
                .with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.accountId").value(accountId.toString()));
  }
}
