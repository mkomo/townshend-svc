package com.mkomo.townshend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.example.config.TownshendSecurityConfig;
import com.mkomo.townshend.controller.TownshendForgottenLoginController;
import com.mkomo.townshend.controller.TownshendInvitationController;
import com.mkomo.townshend.controller.TownshendInvitationRequestController;
import com.mkomo.townshend.controller.TownshendUserController;
import com.mkomo.townshend.security.TownshendTokenFilter;

import lombok.Getter;
import lombok.Setter;

@Configuration
@EnableResourceServer
@ConfigurationProperties(prefix = "townshend.authz")
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Getter @Setter
	private String resourceId;

	@Autowired
	private AuthorizationServerConfig authorizationServerConfig;
	@Autowired
	private TownshendApplicationConfig appConfig;
	@Autowired
	private TownshendSecurityConfig securityConfig;

	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
		converter.setSigningKey(authorizationServerConfig.getSigningKey());
		return converter;
	}

	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(accessTokenConverter());
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) {
		resources.resourceId(this.resourceId).stateless(true).tokenServices(tokenServices());
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry authorizeRequests;
		authorizeRequests = http
			.cors()
		.and()
			.authorizeRequests();

		securityConfig.configurePaths(authorizeRequests)
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.antMatchers("/oauth/**").permitAll()
				.antMatchers("/api/config/**").permitAll()

				//get invitation with code
				.antMatchers(HttpMethod.GET, TownshendInvitationController.PATH + "/*").permitAll() // auth handled by controller permission scheme

				//user create/anon auth
				.antMatchers(HttpMethod.POST, TownshendUserController.USER_PATH).permitAll() //create user
				.antMatchers(HttpMethod.HEAD, TownshendUserController.USER_PATH).permitAll() //check if user exists for create form
				.antMatchers(HttpMethod.POST, TownshendForgottenLoginController.PATH).permitAll() //forgot login
				.antMatchers(HttpMethod.POST, TownshendForgottenLoginController.PATH_RESET).permitAll() //forgot login
				.antMatchers(HttpMethod.POST, TownshendInvitationRequestController.PATH).permitAll() //request invitation
				.anyRequest().authenticated()
		.and()
			.exceptionHandling()
				.accessDeniedHandler(new OAuth2AccessDeniedHandler())
		.and()
			.addFilterBefore(new TownshendTokenFilter(), SecurityContextHolderAwareRequestFilter.class);
	}


	private static final long MAX_AGE_SECONDS = 60 * 60;

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.addAllowedOrigin(String.format("%s://%s",
				appConfig.getWebsiteScheme(), appConfig.getWebsiteHost()));
		if (appConfig.getValidWebsiteHosts() != null) {
			for (String host : appConfig.getValidWebsiteHosts()) {
				config.addAllowedOrigin(String.format("%s://%s",
						appConfig.getWebsiteScheme(), host));
			}
		}
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.setMaxAge(MAX_AGE_SECONDS);
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}

	@Bean
	@Primary
	public DefaultTokenServices tokenServices() {
		DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
		defaultTokenServices.setTokenStore(tokenStore());
		return defaultTokenServices;
	}
}