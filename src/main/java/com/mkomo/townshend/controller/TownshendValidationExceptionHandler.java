package com.mkomo.townshend.controller;

import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.mkomo.townshend.bean.helper.TownshendEntityError;

@ControllerAdvice
public class TownshendValidationExceptionHandler extends ResponseEntityExceptionHandler {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	@ExceptionHandler({ ConstraintViolationException.class })
	public ResponseEntity<TownshendEntityError> handleConstraintValidationException(ConstraintViolationException ex) {
		return ResponseEntity.badRequest().body(this.getErrorFromConstraintViolationException(ex));
	}

	@ExceptionHandler({ TransactionSystemException.class })
	public ResponseEntity<TownshendEntityError> handleTransactionSystemException(TransactionSystemException ex) {
		Throwable rootCause = ex.getRootCause();
		if (ConstraintViolationException.class.isAssignableFrom(rootCause.getClass())) {
			return ResponseEntity.badRequest().body(
					this.getErrorFromConstraintViolationException((ConstraintViolationException)rootCause));
		} else {
			logger.error("TransactionSystemException thrown", ex);
			return ResponseEntity.badRequest().body(
					new TownshendEntityError(rootCause.getLocalizedMessage()));
		}
	}

	@ExceptionHandler({ DataIntegrityViolationException.class })
	public ResponseEntity<TownshendEntityError> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
		logger.error("DataIntegrityViolationException thrown", ex);
		return ResponseEntity.badRequest().body(
				new TownshendEntityError(ex.getRootCause().getLocalizedMessage()));
	}

	@ExceptionHandler({ Exception.class })
	public ResponseEntity<TownshendEntityError> handleExceptionDebug(Exception ex) {
		logger.error("Exception thrown", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new TownshendEntityError(ex.getMessage()));
	}

	private TownshendEntityError getErrorFromConstraintViolationException(ConstraintViolationException ex) {
		return TownshendEntityError.ofUntyped(ex.getConstraintViolations());
	}

}
