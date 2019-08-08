package com.example.config;

import org.springframework.stereotype.Component;

import com.mkomo.townshend.config.TownshendFrontendApplicationConfig;

@Component
public class FrontendApplicationConfig implements TownshendFrontendApplicationConfig {

	@Override
	public Object getFrontendConfig() {
		return null;
	}

}
