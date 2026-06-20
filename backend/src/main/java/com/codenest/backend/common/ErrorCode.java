package com.codenest.backend.common;

public enum ErrorCode {
  BAD_REQUEST(40000),
  UNAUTHORIZED(40001),
  FORBIDDEN(40003),
  NOT_FOUND(40004),
  DUPLICATE(40009),
  SENSITIVE_WORD_BLOCKED(40022),
  SERVER_ERROR(50000);

  private final int code;

  ErrorCode(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }
}
