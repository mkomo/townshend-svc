package com.mkomo.townshend.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TownshendTokenFilter extends GenericFilterBean {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(TownshendTokenFilter.class);
	private static final TypeReference<TownshendClaims> CLAIMS_TYPE_REFERENCE =
			new TypeReference<TownshendClaims>() {};
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		logger.debug("is this filter running???");
		try {

			HttpServletRequest req = (HttpServletRequest) request;
			String token = getJwtFromRequest(req);

			logger.debug("is there a token? " + token);
			if (StringUtils.hasText(token)) {

				Jwt jwt = JwtHelper.decode(token);
				TownshendClaims claims = MAPPER.readValue(jwt.getClaims(),
						CLAIMS_TYPE_REFERENCE);

				logger.debug("are there claims?" + claims);
				Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
				TownshendAuthentication townshendAuth = new TownshendAuthentication(authentication, claims);
				SecurityContextHolder.getContext().setAuthentication(townshendAuth);
			}
		} catch (Exception ex) {
			logger.error("Could not set user authentication in security context", ex);
		}

		chain.doFilter(request, response);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null) {
			bearerToken = bearerToken.trim();
			if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
				return bearerToken.substring(BEARER_PREFIX.length(), bearerToken.length());
			}
		}
		return null;
	}

}
