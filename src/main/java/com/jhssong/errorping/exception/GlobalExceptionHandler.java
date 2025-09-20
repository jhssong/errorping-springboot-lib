package com.jhssong.errorping.exception;

import com.jhssong.errorping.ErrorpingService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorpingService errorpingService;

    private ProblemDetail createProblemDetail(HttpStatus status,
                                              String detail,
                                              HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("method", request.getMethod());
        problem.setProperty("timestamp", LocalDateTime.now().toString());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    String field = error.getField();
                    String failedConstraint = "";

                    String[] codes = error.getCodes();
                    if (codes != null && codes.length > 0) {
                        failedConstraint = codes[codes.length - 1];
                    }

                    return switch (failedConstraint) {
                        case "NotBlank", "NotNull" -> field + " 필드는 필수입니다.";
                        case "Email" -> field + " 형식이 올바르지 않습니다.";
                        default -> field + ": " + error.getDefaultMessage();
                    };
                })
                .distinct()
                .collect(Collectors.joining(", "));

        ProblemDetail problem = createProblemDetail(HttpStatus.BAD_REQUEST, message, request);
        log.error("[ValidationException] {} - {}", HttpStatus.BAD_REQUEST.value(), message);
        errorpingService.sendError(problem);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ProblemDetail problem = createProblemDetail(HttpStatus.METHOD_NOT_ALLOWED,
                "지원되지 않는 요청 메서드입니다.", request);
        log.warn("[MethodNotAllowed] {} - {}", HttpStatus.METHOD_NOT_ALLOWED.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(NoResourceFoundException ex,
                                                               HttpServletRequest request) {
        ProblemDetail problem = createProblemDetail(HttpStatus.NOT_FOUND,
                "요청한 리소스를 찾을 수 없습니다.", request);
        log.warn("[NotFound] {} - {}", HttpStatus.NOT_FOUND.value(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllExceptions(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 에러입니다.", request);
        log.error("[InternalServerError] {}", ex.getMessage(), ex);
        errorpingService.sendError(problem);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(BaseDomainException.class)
    public ResponseEntity<ProblemDetail> handleBaseDomainException(BaseDomainException ex,
                                                                   WebRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request.resolveReference(WebRequest.REFERENCE_REQUEST);
        assert httpRequest != null;
        ProblemDetail problem = createProblemDetail(ex.getStatus(), ex.getMessage(), httpRequest);
        log.error("[BaseDomainException] {} - {}", ex.getStatus().value(), ex.getMessage());
        errorpingService.sendError(problem);
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }
}
