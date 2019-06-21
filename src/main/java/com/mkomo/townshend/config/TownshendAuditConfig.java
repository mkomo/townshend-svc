package com.mkomo.townshend.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.security.TownshendAuthentication;

@Configuration
@EnableJpaRepositories(basePackages = "com.mkomo",
repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@EnableJpaAuditing
public class TownshendAuditConfig {

	@Bean
	public AuditorAware<TownshendUser> auditorProvider() {
		return new SpringSecurityAuditAwareImpl();
	}
}

class SpringSecurityAuditAwareImpl implements AuditorAware<TownshendUser> {

	@Override
	public Optional<TownshendUser> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}

		TownshendAuthentication userPrincipal = (TownshendAuthentication) authentication;

		TownshendUser user = userPrincipal.getClaims().getSub() == null
				? null
				: new TownshendUser(userPrincipal.getClaims());

		return Optional.ofNullable(user);
	}
}