package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.stereotype.Component;

import com.mkomo.townshend.config.TownshendAccountConfig;

@Component
public class TownshendSecurityConfig {

	public AbstractRequestMatcherRegistry<ExpressionUrlAuthorizationConfigurer<HttpSecurity>.AuthorizedUrl> configurePaths(
			ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry authorizeRequests) {
		return authorizeRequests
				;
	}

	@Bean
	public TownshendAccountConfig getAccountConfig() {
		return new TownshendAccountConfig(false, true, true, false, true, false, false);
	}

}
