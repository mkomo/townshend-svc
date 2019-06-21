package com.mkomo.townshend.security;

import java.util.LinkedHashMap;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.mkomo.townshend.config.TownshendFieldConfig;


public class TownshendClaims extends LinkedHashMap<String, Object> {

	/**
	 *
	 */
	private static final long serialVersionUID = -5612382156700151485L;

	public static final String KEY_USER_NAME = "user_name";//TODO make this 3-letter compliant
	public static final String KEY_SUB = "sub";
	public static final String KEY_ISS = "iss";

	public Long getSub() {
		return ((Number) get(KEY_SUB)).longValue();
	}

	public @NotBlank @Size(max = TownshendFieldConfig.USER_MAX_PASSWORD_LENGTH,
			min = TownshendFieldConfig.USER_MIN_PASSWORD_LENGTH) String getUserName() {
		return (String) get(KEY_USER_NAME);
	}

}
