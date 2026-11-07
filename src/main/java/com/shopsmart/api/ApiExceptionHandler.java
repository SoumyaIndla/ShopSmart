package com.shopsmart.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiDtos.ErrorResponse validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(x -> x.getField() + ": " + x.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return error(400, "Bad Request", message);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiDtos.ErrorResponse> status(ResponseStatusException e) {
        int code = e.getStatusCode().value();

        var body = error(
                code,
                HttpStatus.valueOf(code).getReasonPhrase(),
                e.getReason()
        );

        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiDtos.ErrorResponse denied(AccessDeniedException e) {
        return error(403, "Forbidden",
                "You do not have permission to perform this action");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiDtos.ErrorResponse badRequest(IllegalArgumentException e) {
        return error(400, "Bad Request", e.getMessage());
    }

    /*
     * A request body that cannot be parsed (invalid JSON, unknown enum value,
     * wrong field type) is a client error, not a server error.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiDtos.ErrorResponse unreadable(HttpMessageNotReadableException e) {
        return error(400, "Bad Request",
                "Malformed or invalid request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiDtos.ErrorResponse typeMismatch(MethodArgumentTypeMismatchException e) {
        return error(400, "Bad Request",
                "Invalid value for parameter '" + e.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiDtos.ErrorResponse missingParameter(MissingServletRequestParameterException e) {
        return error(400, "Bad Request",
                "Missing required parameter '" + e.getParameterName() + "'");
    }

    /*
     * Spring MVC throws NoResourceFoundException for any URL that does not match
     * a controller or a static file. Without this handler it fell through to the
     * catch-all below and produced a misleading HTTP 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiDtos.ErrorResponse noResource(NoResourceFoundException e) {
        return error(404, "Not Found",
                "The requested resource was not found");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiDtos.ErrorResponse unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return error(415, "Unsupported Media Type",
                "Content-Type must be application/json");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiDtos.ErrorResponse methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return error(405, "Method Not Allowed",
                "HTTP " + e.getMethod() + " is not supported for this endpoint");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiDtos.ErrorResponse general(Exception e) {
        // Log the real cause; the client still receives a generic message
        // so internal details (SQL, class names) are never leaked.
        log.error("Unhandled exception while processing request", e);

        return error(500, "Internal Server Error",
                "The request could not be completed");
    }

    private ApiDtos.ErrorResponse error(int status, String reason, String message) {
        return new ApiDtos.ErrorResponse(
                Instant.now(), status, reason, message, null
        );
    }
}
