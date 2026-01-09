package com.myqyl.aitradex.etrade.alerts.dto;

/**
 * Alert Details DTO from Alert Details API.
 * 
 * According to E*TRADE Alerts API documentation:
 * Contains detailed information about a specific alert.
 */
public class AlertDetailsDto {

  private Long id; // The numeric alert ID (int64)
  private Long createTime; // The date and time the alert was issued, in Epoch time (int64)
  private String subject; // The subject of the alert
  private String msgText; // The text of the alert message
  private Long readTime; // The time the alert was read (int64)
  private Long deleteTime; // The time the alert was deleted (int64)
  private String symbol; // The market symbol for the instrument related to this alert, if any; for example, GOOG. It is set only in case of Stock alerts.
  private String next; // Contains url for next alert
  private String prev; // Contains url for previous alert

  public AlertDetailsDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getMsgText() {
    return msgText;
  }

  public void setMsgText(String msgText) {
    this.msgText = msgText;
  }

  public Long getReadTime() {
    return readTime;
  }

  public void setReadTime(Long readTime) {
    this.readTime = readTime;
  }

  public Long getDeleteTime() {
    return deleteTime;
  }

  public void setDeleteTime(Long deleteTime) {
    this.deleteTime = deleteTime;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getNext() {
    return next;
  }

  public void setNext(String next) {
    this.next = next;
  }

  public String getPrev() {
    return prev;
  }

  public void setPrev(String prev) {
    this.prev = prev;
  }
}
