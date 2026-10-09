package ru.sovmestim.common.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * Maps application and framework errors to a stable {@link ApiError} body. Client-caused failures
 * (bad input, missing resource, unsupported method, conflicting state) keep their 4xx status; the
 * catch-all is reserved for genuinely unexpected errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Machine-readable code for request validation failures. */
    private static final String CODE_VALIDATION_ERROR = "VALIDATION_ERROR";

    /** Message shown for request validation failures. */
    private static final String MESSAGE_VALIDATION_FAILED = "Request validation failed";

    /** Separator between a field path and its message in validation details. */
    private static final String DETAIL_SEPARATOR = ": ";

    /**
     * Maps an application error to its HTTP status and error body.
     *
     * @param ex the application exception
     * @return the mapped response
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        LOG.debug("API error {}: {}", ex.status(), ex.getMessage());
        return ResponseEntity.status(ex.status()).body(ApiError.of(ex.status().name(), ex.getMessage()));
    }

    /**
     * Maps bean-validation failures to a 400 response with per-field details.
     *
     * @param ex the validation failure raised by the request body binding
     * @return the mapped response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + DETAIL_SEPARATOR + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of(CODE_VALIDATION_ERROR, MESSAGE_VALIDATION_FAILED, details));
    }

    /**
     * Maps method-level constraint violations to a 400 response with per-field details.
     *
     * @param ex the constraint violations raised by method validation
     * @return the mapped response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + DETAIL_SEPARATOR + violation.getMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of(CODE_VALIDATION_ERROR, MESSAGE_VALIDATION_FAILED, details));
    }

    /**
     * Maps an unreadable request body to a 400 response.
     *
     * @param ex the body-binding failure
     * @return the mapped response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        LOG.debug("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiError.of("MALFORMED_REQUEST", "Request body could not be read"));
    }

    /**
     * Maps a path variable or query parameter with the wrong type to a 400 response.
     *
     * @param ex the type-mismatch failure
     * @return the mapped response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Parameter '" + ex.getName() + "' has an invalid value";
        return ResponseEntity.badRequest().body(ApiError.of("INVALID_PARAMETER", message));
    }

    /**
     * Maps a missing required request parameter to a 400 response.
     *
     * @param ex the missing-parameter failure
     * @return the mapped response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        return ResponseEntity.badRequest().body(ApiError.of("MISSING_PARAMETER", message));
    }

    /**
     * Maps a database integrity violation (for example a duplicate key) to a 409 response.
     *
     * @param ex the integrity violation
     * @return the mapped response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        LOG.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("CONFLICT", "The request conflicts with existing data"));
    }

    /**
     * Maps an unknown resource path to a 404 response.
     *
     * @param ex the not-found failure
     * @return the mapped response
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("NOT_FOUND", "Resource not found"));
    }

    /**
     * Maps an unsupported HTTP method to a 405 response.
     *
     * @param ex the method-not-supported failure
     * @return the mapped response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of("METHOD_NOT_ALLOWED", ex.getMessage()));
    }

    /**
     * Maps illegal arguments to a 400 response.
     *
     * @param ex the illegal argument exception
     * @return the mapped response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", ex.getMessage()));
    }

    /**
     * Logs and maps any unhandled exception to a 500 response.
     *
     * @param ex the unexpected exception
     * @return the mapped response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        LOG.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Unexpected error"));
    }
}
