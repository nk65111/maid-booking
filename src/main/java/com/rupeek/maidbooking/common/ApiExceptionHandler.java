package com.rupeek.maidbooking.common;

import java.util.stream.Collectors;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(int status, String message) {
    }

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ApiError> handleValidation(ValidationException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Value objects validate themselves in their constructors, so when Jackson builds one from
     * the request body our ValidationException ends up wrapped. Surface its message.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ValidationException validation) {
                return error(HttpStatus.BAD_REQUEST, validation.getMessage());
            }
        }
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage()).lines().findFirst().orElse("");
        return error(HttpStatus.BAD_REQUEST, "Malformed request body: " + detail);
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiError> handleBind(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(e -> e.getField() + " has invalid value '" + e.getRejectedValue() + "'")
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({TypeMismatchException.class, ServletRequestBindingException.class})
    ResponseEntity<ApiError> handleBadParameter(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message));
    }
}
