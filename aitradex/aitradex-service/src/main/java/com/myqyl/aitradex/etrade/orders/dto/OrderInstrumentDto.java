package com.myqyl.aitradex.etrade.orders.dto;

/**
 * Instrument DTO for Order.
 * 
 * According to E*TRADE Order API documentation:
 * Represents the instrument (security + action + quantity) in an order.
 */
public class OrderInstrumentDto {

  private OrderProductDto product;
  private String orderAction; // BUY, SELL, SELL_SHORT, BUY_TO_COVER
  private Integer quantity;
  private String quantityType; // QUANTITY, DOLLARS, DOLLARS_NO_FRACTIONAL_SHARES
  private Double averageExecutionPrice; // For executed orders
  private Integer reservedQuantity;
  private Integer filledQuantity;
  private Integer remainingQuantity;

  public OrderInstrumentDto() {
  }

  public OrderProductDto getProduct() {
    return product;
  }

  public void setProduct(OrderProductDto product) {
    this.product = product;
  }

  public String getOrderAction() {
    return orderAction;
  }

  public void setOrderAction(String orderAction) {
    this.orderAction = orderAction;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public String getQuantityType() {
    return quantityType;
  }

  public void setQuantityType(String quantityType) {
    this.quantityType = quantityType;
  }

  public Double getAverageExecutionPrice() {
    return averageExecutionPrice;
  }

  public void setAverageExecutionPrice(Double averageExecutionPrice) {
    this.averageExecutionPrice = averageExecutionPrice;
  }

  public Integer getReservedQuantity() {
    return reservedQuantity;
  }

  public void setReservedQuantity(Integer reservedQuantity) {
    this.reservedQuantity = reservedQuantity;
  }

  public Integer getFilledQuantity() {
    return filledQuantity;
  }

  public void setFilledQuantity(Integer filledQuantity) {
    this.filledQuantity = filledQuantity;
  }

  public Integer getRemainingQuantity() {
    return remainingQuantity;
  }

  public void setRemainingQuantity(Integer remainingQuantity) {
    this.remainingQuantity = remainingQuantity;
  }
}
