package com.mkomo.townshend.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

public class TownshendTokenEnhancer implements TokenEnhancer {

	private final String issuer;

	public TownshendTokenEnhancer(String issuer) {
		this.issuer = issuer;
	}

	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken,
			OAuth2Authentication authentication) {

		TownshendUserDetails user = (TownshendUserDetails)authentication.getPrincipal();

		Map<String, Object> additionalInfo = new HashMap<>();
		additionalInfo.put(TownshendClaims.KEY_ISS, issuer);
		additionalInfo.put(TownshendClaims.KEY_SUB, user.getUserId());

		((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);

		return accessToken;
	}

}
