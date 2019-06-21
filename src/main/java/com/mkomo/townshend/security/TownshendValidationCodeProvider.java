package com.mkomo.townshend.security;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class TownshendValidationCodeProvider {

	public String generateCode() {
		//TODO choose something more secure?
		return UUID.randomUUID().toString();
	}

}
