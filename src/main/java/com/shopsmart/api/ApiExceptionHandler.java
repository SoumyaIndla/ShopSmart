package com.shopsmart.api;
import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;import java.time.Instant;import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public ApiDtos.ErrorResponse validation(MethodArgumentNotValidException e){String message=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).distinct().collect(Collectors.joining("; "));return new ApiDtos.ErrorResponse(Instant.now(),400,"Bad Request",message,null);}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ApiDtos.ErrorResponse> status(ResponseStatusException e){int code=e.getStatusCode().value();var body=new ApiDtos.ErrorResponse(Instant.now(),code,HttpStatus.valueOf(code).getReasonPhrase(),e.getReason(),null);return ResponseEntity.status(e.getStatusCode()).body(body);}
 @ExceptionHandler(AccessDeniedException.class) @ResponseStatus(HttpStatus.FORBIDDEN) public ApiDtos.ErrorResponse denied(AccessDeniedException e){return new ApiDtos.ErrorResponse(Instant.now(),403,"Forbidden","You do not have permission to perform this action",null);}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public ApiDtos.ErrorResponse badRequest(IllegalArgumentException e){return new ApiDtos.ErrorResponse(Instant.now(),400,"Bad Request",e.getMessage(),null);}
 @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) public ApiDtos.ErrorResponse general(Exception e){return new ApiDtos.ErrorResponse(Instant.now(),500,"Internal Server Error","The request could not be completed",null);}
}
