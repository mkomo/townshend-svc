package com.mkomo.townshend.security;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import lombok.ToString;

@ToString
public class TownshendAuthentication implements Authentication {

	private static final long serialVersionUID = -670790688519547727L;

	public static final String ROLE_NAME_ADMIN = "ADMIN";

	private Authentication authentication;
	//TODO make this an object so that we can call methods like getSub, getAuds, getScopes
	private TownshendClaims claims;

	public TownshendAuthentication(Authentication authentication, TownshendClaims claims) {
		this.authentication = authentication;
		this.claims = claims;
	}

	public TownshendClaims getClaims() {
		return this.claims;
	}


	/**
	 * Authentication object method overrides
	 */

	@Override
	public String getName() {
		return authentication.getName();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authentication.getAuthorities();
	}

	@Override
	public Object getCredentials() {
		return authentication.getCredentials();
	}

	@Override
	public Object getDetails() {
		return authentication.getDetails();
	}

	@Override
	public Object getPrincipal() {
		return authentication.getPrincipal();
	}

	@Override
	public boolean isAuthenticated() {
		return authentication.isAuthenticated();
	}

	@Override
	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
		authentication.setAuthenticated(isAuthenticated);

	}

	public boolean isAdmin() {
		return authentication.getAuthorities()
				.stream().map(auth->auth.getAuthority()).collect(Collectors.toList()).contains(ROLE_NAME_ADMIN);
	}

}
