package com.mkomo.townshend.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class TownshendFieldConfig extends AbstractPublicConfig {
	public static final int USER_MAX_NAME_LENGTH = 250;
	public static final int USER_MIN_NAME_LENGTH = 1;

	public static final int USER_MAX_USERNAME_LENGTH = 60;
	public static final int USER_MIN_USERNAME_LENGTH = 4;

	public static final int USER_MAX_EMAIL_LENGTH = 254;
	public static final int USER_MIN_EMAIL_LENGTH = 5;

	public static final int USER_MAX_PASSWORD_LENGTH = 250;
	public static final int USER_MIN_PASSWORD_LENGTH = 6;

}
