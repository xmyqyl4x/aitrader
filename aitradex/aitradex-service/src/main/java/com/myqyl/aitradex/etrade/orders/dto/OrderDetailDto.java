package com.myqyl.aitradex.etrade.orders.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * OrderDetail DTO for Order.
 * 
 * According to E*TRADE Order API documentation:
 * Contains order execution details including price type, term, and instruments.
 */
public class OrderDetailDto {

  private Boolean allOrNone;
  private String priceType; // MARKET, LIMIT, STOP, STOP_LIMIT
  private String orderTerm; // GOOD_FOR_DAY, GOOD_UNTIL_CANCEL, IMMEDIATE_OR_CANCEL, FILL_OR_KILL
  private String marketSession; // REGULAR, EXTENDED
  private Double limitPrice;
  private Double stopPrice;
  private Double stopLimitPrice;
  private Double estimatedCommission;
  private Double estimatedTotalAmount;
  private Double orderValue;
  private List<OrderInstrumentDto> instruments;

  public OrderDetailDto() {
    this.instruments = new ArrayList<>();
  }

  public Boolean getAllOrNone() {
    return allOrNone;
  }

  public void setAllOrNone(Boolean allOrNone) {
    this.allOrNone = allOrNone;
  }

  public String getPriceType() {
    return priceType;
  }

  public void setPriceType(String priceType) {
    this.priceType = priceType;
  }

  public String getOrderTerm() {
    return orderTerm;
  }

  public void setOrderTerm(String orderTerm) {
    this.orderTerm = orderTerm;
  }

  public String getMarketSession() {
    return marketSession;
  }

  public void setMarketSession(String marketSession) {
    this.marketSession = marketSession;
  }

  public Double getLimitPrice() {
    return limitPrice;
  }

  public void setLimitPrice(Double limitPrice) {
    this.limitPrice = limitPrice;
  }

  public Double getStopPrice() {
    return stopPrice;
  }

  public void setStopPrice(Double stopPrice) {
    this.stopPrice = stopPrice;
  }

  public Double getStopLimitPrice() {
    return stopLimitPrice;
  }

  public void setStopLimitPrice(Double stopLimitPrice) {
    this.stopLimitPrice = stopLimitPrice;
  }

  public Double getEstimatedCommission() {
    return estimatedCommission;
  }

  public void setEstimatedCommission(Double estimatedCommission) {
    this.estimatedCommission = estimatedCommission;
  }

  public Double getEstimatedTotalAmount() {
    return estimatedTotalAmount;
  }

  public void setEstimatedTotalAmount(Double estimatedTotalAmount) {
    this.estimatedTotalAmount = estimatedTotalAmount;
  }

  public Double getOrderValue() {
    return orderValue;
  }

  public void setOrderValue(Double orderValue) {
    this.orderValue = orderValue;
  }

  public List<OrderInstrumentDto> getInstruments() {
    return instruments;
  }

  public void setInstruments(List<OrderInstrumentDto> instruments) {
    this.instruments = instruments != null ? instruments : new ArrayList<>();
  }
}
