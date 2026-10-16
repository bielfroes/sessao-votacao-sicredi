package com.froes.assembly.resource.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import com.froes.assembly.integration.ExternalEligibilityServiceException;
import com.froes.assembly.integration.ExternalVoterNotFoundException;
import com.froes.assembly.integration.InvalidEligibilityResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorDTO> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(error(status, safeMessage(exception), request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDTO> handleResourceNotFound(
            NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, "Recurso não encontrado", request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDTO> handleMethodNotAllowed(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(error(HttpStatus.METHOD_NOT_ALLOWED,
                        "Método HTTP não suportado para este recurso", request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleInvalidArguments(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldMessage)
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, message, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDTO> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, message, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> handleUnreadableBody(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST,
                "O corpo da requisição está malformado ou contém valores inválidos", request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> handleTypeMismatch(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST,
                "Um parâmetro da requisição possui um valor inválido", request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> handleDataConflict(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Uma restrição do banco rejeitou uma requisição para {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(HttpStatus.CONFLICT,
                "A requisição entra em conflito com dados existentes", request));
    }

    @ExceptionHandler(ExternalVoterNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleExternalVoterNotFound(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(HttpStatus.NOT_FOUND,
                "O serviço externo de elegibilidade não encontrou este CPF", request));
    }

    @ExceptionHandler(ExternalEligibilityServiceException.class)
    public ResponseEntity<ErrorDTO> handleExternalServiceUnavailable(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error(HttpStatus.SERVICE_UNAVAILABLE,
                "O serviço de elegibilidade de eleitores está indisponível", request));
    }

    @ExceptionHandler(InvalidEligibilityResponseException.class)
    public ResponseEntity<ErrorDTO> handleInvalidExternalResponse(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(HttpStatus.BAD_GATEWAY,
                "O serviço de elegibilidade de eleitores retornou uma resposta inválida", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), exception);
        return ResponseEntity.internalServerError().body(error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado", request));
    }

    private ErrorDTO error(HttpStatus status, String message, HttpServletRequest request) {
        return ErrorDTO.builder()
                .status(status.value())
                .error(status.name())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }

    private String safeMessage(ResponseStatusException exception) {
        return exception.getReason() == null ? exception.getStatusCode().toString() : exception.getReason();
    }

    private String fieldMessage(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
