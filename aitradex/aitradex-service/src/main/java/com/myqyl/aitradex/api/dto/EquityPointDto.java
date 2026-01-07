package com.myqyl.aitradex.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquityPointDto(LocalDate asOfDate, BigDecimal equity, BigDecimal drawdown) {}
