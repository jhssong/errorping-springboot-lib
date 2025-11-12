package com.jhssong.errorping.exception;

import static org.slf4j.LoggerFactory.getLogger;

import com.jhssong.errorping.ErrorpingService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger EXCEPTION_DETAIL_LOGGER = getLogger("ERROR_DETAIL_LOGGER");
    private final ErrorpingService errorpingService;

    private ProblemDetail createProblemDetail(HttpStatus status,
                                              String detail,
                                              HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("method", request.getMethod());
        problem.setProperty("timestamp", ZonedDateTime.now().toOffsetDateTime().toString());
        return problem;
    }

    private void logDetailedException(Exception ex) {
        StackTraceElement[] origin = ex.getStackTrace();
        EXCEPTION_DETAIL_LOGGER.error(
                "Exception occurred at {}.{}({}:{}): {}",
                origin[0].getClassName(),
                origin[0].getMethodName(),
                origin[0].getFileName(),
                origin[0].getLineNumber(),
                ex.getMessage(),
                ex
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        ProblemDetail problem = createProblemDetail(HttpStatus.FORBIDDEN,
                "권한이 필요합니다.", request);
        log.warn("[Forbidden] status={} method={} uri={} message={}",
                HttpStatus.FORBIDDEN.value(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
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
        log.warn("[ValidationException] status={} method={} uri={} message={}",
                HttpStatus.BAD_REQUEST.value(),
                request.getMethod(),
                request.getRequestURI(),
                message);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";
        String message = String.format("'%s' 파라미터는 %s 타입이어야 합니다.", paramName, requiredType);

        ProblemDetail problem = createProblemDetail(HttpStatus.BAD_REQUEST, message, request);
        log.warn("[TypeMismatch] status={} method={} uri={} message={}",
                HttpStatus.BAD_REQUEST.value(),
                request.getMethod(),
                request.getRequestURI(),
                message);

        return ResponseEntity.badRequest().body(problem);
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ProblemDetail problem = createProblemDetail(HttpStatus.METHOD_NOT_ALLOWED,
                "지원되지 않는 요청 메서드입니다.", request);
        log.warn("[MethodNotAllowed] status={} method={} uri={} message={}",
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFound(NoResourceFoundException ex,
                                                               HttpServletRequest request) {
        ProblemDetail problem = createProblemDetail(HttpStatus.NOT_FOUND,
                "요청한 리소스를 찾을 수 없습니다.", request);
        log.warn("[NotFound] status={} method={} uri={} message={}",
                HttpStatus.NOT_FOUND.value(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllExceptions(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 에러입니다.", request);
        log.error("[InternalServerError] status={} method={} uri={} message={}",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        logDetailedException(ex);

        errorpingService.sendError(problem);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(BaseDomainException.class)
    public ResponseEntity<ProblemDetail> handleBaseDomainException(BaseDomainException ex,
                                                                   HttpServletRequest request) {
        ProblemDetail problem = createProblemDetail(ex.getStatus(), ex.getMessage(), request);
        if (ex.getStatus().is5xxServerError()) {
            log.error("[{}] status={} method={} uri={} message={}",
                    ex.getClass().getSimpleName(),
                    ex.getStatus().value(),
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getMessage());
            logDetailedException(ex);
            errorpingService.sendError(problem);
        } else {
            log.warn("[{}] status={} method={} uri={} message={}",
                    ex.getClass().getSimpleName(),
                    ex.getStatus().value(),
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getMessage());
        }

        return ResponseEntity.status(ex.getStatus()).body(problem);
    }
}
