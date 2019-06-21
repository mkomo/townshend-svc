package com.mkomo.townshend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import com.mkomo.townshend.security.TownshendTokenEnhancer;

import lombok.Getter;
import lombok.Setter;

@Configuration
@EnableAuthorizationServer
@ConfigurationProperties(prefix = "townshend.authz")
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

	@Getter @Setter
	private String signingKey;
	@Getter @Setter
	private String issuerName;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private ResourceServerConfig resourceServerConfig;

	@Autowired
	private SecurityConfig securityConfig;

	//TODO change this to come from config; implement refresh tokens from FE
	private int accessTokenValiditySeconds = 60*60*24*7;

	//TODO social login https://github.com/callicoder/spring-boot-react-oauth2-social-login-demo

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		//TODO get all of this into config
		clients
				.inMemory()
				.withClient("trusted-app")
				.secret(securityConfig.passwordEncoder().encode("secret"))
				.authorizedGrantTypes("client_credentials", "password", "refresh_token", "implicit")
				.authorities("ROLE_TRUSTED_CLIENT")
				.scopes("app")
				.accessTokenValiditySeconds(accessTokenValiditySeconds)
				.resourceIds(resourceServerConfig.getResourceId());
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
		tokenEnhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer(),
				accessTokenConverter()));

		endpoints.tokenStore(tokenStore()).tokenEnhancer(tokenEnhancerChain)
				.authenticationManager(authenticationManager);
	}

	@Bean
	public TokenEnhancer tokenEnhancer() {
		return new TownshendTokenEnhancer(this.issuerName);
	}

	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(accessTokenConverter());
	}

	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
		converter.setSigningKey(this.signingKey);
		return converter;
	}

	@Bean
	@Primary
	public DefaultTokenServices tokenServices() {
		DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
		defaultTokenServices.setTokenStore(tokenStore());
		defaultTokenServices.setSupportRefreshToken(true);
		return defaultTokenServices;
	}

}