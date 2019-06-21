package com.mkomo.townshend.bean.helper;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class PasswordResetRequest {
	private Long id;
	private String code;
	private String password;
}
