package br.com.grifo.core.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        String message = getTranslatedMessage(
                exception.getMessageKey(),
                exception.getArgs(),
                "Erro de regra de negócio: " + exception.getMessageKey(),
                request
        );
        return buildErrorResponse(exception.getHttpStatus(), message, request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> validationErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Campo inválido",
                        (existing, ignored) -> existing
                ));

        String message = getTranslatedMessage(
                "error.validation.generic",
                null,
                "Erro de validação nos dados fornecidos.",
                request
        );
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request, validationErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAllUncaughtException(Exception exception, HttpServletRequest request) {
        log.error("Erro interno não tratado interceptado na URI: {}", request.getRequestURI(), exception);
        String message = getTranslatedMessage(
                "error.server.internal",
                null,
                "Ocorreu um erro interno inesperado no servidor. Contate o suporte.",
                request
        );
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request, null);
    }

    private String getTranslatedMessage(String key, Object[] args, String defaultMessage, HttpServletRequest request) {
        return messageSource.getMessage(key, args, defaultMessage, request.getLocale());
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> validationErrors) {

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}
