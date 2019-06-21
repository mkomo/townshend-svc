package com.mkomo.townshend.bean.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolation;
import javax.validation.Path;

import org.hibernate.validator.internal.engine.path.PathImpl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class TownshendEntityError {

	public static TownshendEntityError fieldError(String fieldName, String errorText) {
		Map<Path, String> fieldErrs =
				new LinkedHashMap<Path, String>(
						Collections.singletonMap(PathImpl.createPathFromString(fieldName), errorText));

		return new TownshendEntityError(fieldErrs, null);
	}

	public static <T> TownshendEntityError of(Collection<ConstraintViolation<T>> violations) {
		Map<Path, String> errors = new LinkedHashMap<>();
		for (ConstraintViolation<?> violation : violations) {
			errors.put(violation.getPropertyPath(), violation.getMessage());
		}
		return new TownshendEntityError(errors, null);
	}

	public static TownshendEntityError ofUntyped(Collection<ConstraintViolation<?>> violations) {
		Map<Path, String> errors = new LinkedHashMap<>();
		for (ConstraintViolation<?> violation : violations) {
			errors.put(violation.getPropertyPath(), violation.getMessage());
		}
		return new TownshendEntityError(errors, null);
	}

	Map<Path, String> fieldErrors;
	List<String> generalErrors;

	public TownshendEntityError(String... generalErrors) {
		this.generalErrors = new ArrayList<String>(Arrays.asList(generalErrors));
	}
}
