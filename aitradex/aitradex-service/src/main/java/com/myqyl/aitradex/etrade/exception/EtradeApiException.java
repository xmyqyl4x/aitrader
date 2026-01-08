package com.myqyl.aitradex.etrade.exception;

/**
 * Custom exception for E*TRADE API errors.
 * Provides structured error information with HTTP status codes and error codes.
 */
public class EtradeApiException extends RuntimeException {

  private final int httpStatus;
  private final String errorCode;
  private final String errorMessage;

  public EtradeApiException(int httpStatus, String errorCode, String errorMessage) {
    super(errorMessage);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  public EtradeApiException(int httpStatus, String errorCode, String errorMessage, Throwable cause) {
    super(errorMessage, cause);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String toString() {
    return String.format("EtradeApiException[status=%d, code=%s, message=%s]",
        httpStatus, errorCode, errorMessage);
  }
}
