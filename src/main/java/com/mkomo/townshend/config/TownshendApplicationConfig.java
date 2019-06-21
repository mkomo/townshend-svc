package com.mkomo.townshend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "townshend.application")
public class TownshendApplicationConfig {
	public static final String API_BASE_PATH = "/api";

	private String websiteScheme;
	private String websiteHost;
	private String[] validWebsiteHosts;

	private String websiteResetPasswordPath;
	private String websiteResetIdParam;
	private String websiteResetCodeParam;

	private String websiteAcceptInvitePath;
	private String websiteInviteIdParam;
	private String websiteInviteCodeParam;
	private String websiteInviteEmailParam;

	private boolean debugEmail;
	private String debugEmailAddress;
	private String automatedEmailLocalPart;
	private String automatedEmailHost;

	public String getAutomatedEmailAddress() {
		return this.getAutomatedEmailAddress(null);
	}

	public String getAutomatedEmailAddress(String wildCard) {
		return (wildCard != null)
			? String.format("%s+%s@%s", automatedEmailLocalPart, wildCard, automatedEmailHost)
			: String.format("%s@%s", automatedEmailLocalPart, automatedEmailHost);
	}
}
