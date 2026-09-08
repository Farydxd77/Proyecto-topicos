package com.cuentasclaras.backend.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new TreeMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(),
                    fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "inválido");
        }

        Map<String, Object> body = standardBody(HttpStatus.BAD_REQUEST,
                "Uno o más campos son inválidos", request);
        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameExists(
            UsernameAlreadyExistsException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(standardBody(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(standardBody(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", request));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(standardBody(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(
            ForbiddenOperationException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(standardBody(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            ConflictException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(standardBody(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(standardBody(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    @ExceptionHandler(ServicioExternoNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleServicioExterno(
            ServicioExternoNoDisponibleException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(standardBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request));
    }

    // --- Fallos de infraestructura web ----------------------------------

    /** Cuerpo ausente, JSON malformado, o un valor cuyo tipo no corresponde al campo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleCuerpoIlegible(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest()
                .body(standardBody(HttpStatus.BAD_REQUEST,
                        "El cuerpo de la petición es inválido", request));
    }

    /** Parámetro de ruta o de query que no se puede convertir al tipo declarado. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoDeParametro(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest()
                .body(standardBody(HttpStatus.BAD_REQUEST,
                        "El parámetro '" + ex.getName() + "' tiene un valor inválido", request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMetodoNoPermitido(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(standardBody(HttpStatus.METHOD_NOT_ALLOWED,
                        "El método " + ex.getMethod() + " no está permitido en esta ruta", request));
    }

    /**
     * Ruta inexistente. Según cómo resuelva el despacho, Spring lanza una u otra:
     * {@code NoResourceFoundException} cuando la petición cae en el manejador de
     * recursos estáticos, {@code NoHandlerFoundException} cuando no hay ninguno.
     */
    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<Map<String, Object>> handleRutaInexistente(
            Exception ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(standardBody(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", request));
    }

    /**
     * Restricción de la base de datos que la validación no anticipó. El mensaje es
     * genérico a propósito: el detalle nombra tablas, restricciones y SQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridad(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Violación de integridad en {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(standardBody(HttpStatus.CONFLICT,
                        "La operación viola una restricción de integridad de datos", request));
    }

    /**
     * Rechazo de la autorización declarativa de Spring Security. Hoy la autorización
     * vive en los servicios y usa {@link ForbiddenOperationException}; este handler
     * queda como red para el día que se agregue una regla declarativa.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegado(
            AccessDeniedException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(standardBody(HttpStatus.FORBIDDEN,
                        "No tienes permisos para realizar esta operación", request));
    }

    /**
     * Último recurso. Spring elige siempre el handler más específico, así que este
     * solo atrapa lo que ninguno de los anteriores contempla. La excepción completa
     * va al log; la respuesta no revela nada de ella.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleImprevisto(
            Exception ex, HttpServletRequest request) {

        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(standardBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Ha ocurrido un error inesperado", request));
    }

    private Map<String, Object> standardBody(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return body;
    }
}
