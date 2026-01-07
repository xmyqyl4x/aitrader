package com.myqyl.aitradex.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myqyl.aitradex.config.SecurityConfig;
import com.myqyl.aitradex.security.JwtTokenService;
import com.myqyl.aitradex.service.StopLossService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RiskController.class)
@Import(SecurityConfig.class)
class RiskControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StopLossService stopLossService;

  @MockBean private JwtTokenService jwtTokenService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void runStopLossesReturnsCount() throws Exception {
    when(stopLossService.enforceStopLosses(null)).thenReturn(2);

    mockMvc
        .perform(post("/api/risk/stop-loss/run").with(csrf()))
        .andExpect(status().isAccepted())
        .andExpect(content().string("2"));
  }
}
