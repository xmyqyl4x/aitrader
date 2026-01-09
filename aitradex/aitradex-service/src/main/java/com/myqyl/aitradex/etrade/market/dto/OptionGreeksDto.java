package com.myqyl.aitradex.etrade.market.dto;

/**
 * Option Greeks DTO for Quote.
 * 
 * According to E*TRADE Market API documentation:
 * Represents the option Greeks (delta, gamma, theta, vega, rho, IV).
 */
public class OptionGreeksDto {

  private Double rho;
  private Double vega;
  private Double theta;
  private Double delta;
  private Double gamma;
  private Double iv; // Implied Volatility
  private Boolean currentValue;

  public OptionGreeksDto() {
  }

  public Double getRho() {
    return rho;
  }

  public void setRho(Double rho) {
    this.rho = rho;
  }

  public Double getVega() {
    return vega;
  }

  public void setVega(Double vega) {
    this.vega = vega;
  }

  public Double getTheta() {
    return theta;
  }

  public void setTheta(Double theta) {
    this.theta = theta;
  }

  public Double getDelta() {
    return delta;
  }

  public void setDelta(Double delta) {
    this.delta = delta;
  }

  public Double getGamma() {
    return gamma;
  }

  public void setGamma(Double gamma) {
    this.gamma = gamma;
  }

  public Double getIv() {
    return iv;
  }

  public void setIv(Double iv) {
    this.iv = iv;
  }

  public Boolean getCurrentValue() {
    return currentValue;
  }

  public void setCurrentValue(Boolean currentValue) {
    this.currentValue = currentValue;
  }
}
