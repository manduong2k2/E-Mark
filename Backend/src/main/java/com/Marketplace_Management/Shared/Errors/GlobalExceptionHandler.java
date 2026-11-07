package com.Marketplace_Management.Shared.Errors;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.Marketplace_Management.Marketplace_ManagementApplication;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;

/**
 * Error bodies: { message } for expected errors; validation adds { errors: {
 * field: message } }.
 * Unexpected errors are logged; their details (class, cause, stack trace) are
 * only returned with app.debug=true.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String BASE_PACKAGE = Marketplace_ManagementApplication.class.getPackageName();

	@Value("${app.debug:false}")
	private boolean debug;

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
		return message(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
			return message(HttpStatus.UNAUTHORIZED, "Unauthenticated");
		}
		return message(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
		return message(ex.getStatusCode(), ex.getReason());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
		return message(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Void> handleNotFound(NoResourceFoundException ex) {

		return ResponseEntity.notFound().build();
	}

	/**
	 * Bad input, and domain rules a request broke (e.g. "Item not found in cart",
	 * "Cannot checkout an empty cart").
	 */
	@ExceptionHandler({ BadRequestException.class, IllegalArgumentException.class, IllegalStateException.class })
	public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
		return message(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/** Storing an upload failed (disk, permissions...). */
	@ExceptionHandler(IOException.class)
	public ResponseEntity<Map<String, Object>> handleIo(IOException ex) {
		logger.error("I/O error", ex);
		return message(HttpStatus.INTERNAL_SERVER_ERROR, "Could not process the uploaded file. Please try again.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

		Map<String, Object> response = new HashMap<>();
		response.put("message", "Validation failed");
		response.put("errors", errors);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		logger.error("Unhandled exception", ex);

		ApiError.ApiErrorBuilder error = ApiError.builder()
				.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(debug ? ex.getMessage() : "An unexpected error occurred. Please try again later.");
		if (debug) {
			List<String> trace = Arrays.stream(ex.getStackTrace()).map(StackTraceElement::toString).toList();
			error.className(ex.getClass().getName())
					.cause(ex.getCause() != null ? ex.getCause().getMessage() : null)
					.fullTrace(trace)
					.appTrace(trace.stream().filter(line -> line.startsWith(BASE_PACKAGE)).toList());
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.build());
	}

	private static ResponseEntity<Map<String, Object>> message(HttpStatusCode status, String message) {
		Map<String, Object> body = new HashMap<>();
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}
