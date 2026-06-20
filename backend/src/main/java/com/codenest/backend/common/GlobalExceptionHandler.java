package com.codenest.backend.common;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final String BAD_REQUEST_MESSAGE = "Bad request";
  private static final String METHOD_NOT_ALLOWED_MESSAGE = "Method not allowed";
  private static final String NOT_FOUND_MESSAGE = "Not found";
  private static final String SERVER_ERROR_MESSAGE = "Internal server error";

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    return ResponseEntity.status(httpStatus(exception.errorCode()))
        .body(ApiResponse.error(exception.errorCode(), exception.getMessage()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    BindException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ErrorCode.BAD_REQUEST, BAD_REQUEST_MESSAGE));
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ErrorCode.NOT_FOUND, NOT_FOUND_MESSAGE));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException exception) {
    Set<HttpMethod> supportedMethods = exception.getSupportedHttpMethods();
    ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
    if (supportedMethods != null) {
      responseBuilder.allow(supportedMethods.toArray(HttpMethod[]::new));
    }
    return responseBuilder.body(ApiResponse.error(ErrorCode.BAD_REQUEST, METHOD_NOT_ALLOWED_MESSAGE));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
    if (exception instanceof ErrorResponse errorResponse) {
      return handleErrorResponse(errorResponse);
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ErrorCode.SERVER_ERROR, SERVER_ERROR_MESSAGE));
  }

  private ResponseEntity<ApiResponse<Void>> handleErrorResponse(ErrorResponse errorResponse) {
    HttpStatusCode statusCode = errorResponse.getStatusCode();
    if (statusCode.is4xxClientError()) {
      ErrorCode errorCode =
          statusCode.value() == HttpStatus.NOT_FOUND.value()
              ? ErrorCode.NOT_FOUND
              : ErrorCode.BAD_REQUEST;
      String message = errorCode == ErrorCode.NOT_FOUND ? NOT_FOUND_MESSAGE : BAD_REQUEST_MESSAGE;
      return ResponseEntity.status(statusCode).body(ApiResponse.error(errorCode, message));
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ErrorCode.SERVER_ERROR, SERVER_ERROR_MESSAGE));
  }

  private HttpStatus httpStatus(ErrorCode errorCode) {
    return switch (errorCode) {
      case BAD_REQUEST, SENSITIVE_WORD_BLOCKED -> HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case DUPLICATE -> HttpStatus.CONFLICT;
      case SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
