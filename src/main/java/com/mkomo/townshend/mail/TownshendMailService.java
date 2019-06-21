package com.mkomo.townshend.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.mkomo.townshend.bean.TownshendForgottenLogin;
import com.mkomo.townshend.bean.TownshendInvitation;
import com.mkomo.townshend.config.TownshendApplicationConfig;

@Component
public class TownshendMailService {

	@Autowired
	public JavaMailSender emailSender;
	@Autowired
	public TownshendApplicationConfig appConfig;

	private String fromUserExtensionForgot = "forgot";
	private String forgotSubject() {
		return appConfig.getWebsiteHost() + ": password reset";
	}

	private String fromUserExtensionInvite = "invite";
	private String inviteSubject() {
		return appConfig.getWebsiteHost() + ": you're invited";
	}

	public void send(String to, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		this.sendSimpleMessage(message);
	}

	public void sendSimpleMessage(SimpleMailMessage message) {
		if (appConfig.isDebugEmail()) {
			String[] to = message.getTo();
			message.setTo(appConfig.getDebugEmailAddress());
			message.setSubject("[TO: " + String.join(";", to) + "] " + message.getSubject());
		}
		emailSender.send(message);
	}

	public SimpleMailMessage passwordResetMessage(TownshendForgottenLogin item) {
		SimpleMailMessage message = new SimpleMailMessage();

		String url = getUrlBuilder()
				.path(appConfig.getWebsiteResetPasswordPath())
				.queryParam(appConfig.getWebsiteResetIdParam(), item.getId())
				.queryParam(appConfig.getWebsiteResetCodeParam(), item.getCode())
				.build().toUriString();

		message.setTo(item.getResultantUser().getEmail());
		message.setText(String.format("To reset your password, click on the link below.\n\n"
				+ "If you did not request a password reset, you can disregard this message.\n\n"
				+ "%s\n\n"
				+ "%s", url, appConfig.getWebsiteHost()));
		message.setFrom(appConfig.getAutomatedEmailAddress());
		message.setReplyTo(appConfig.getAutomatedEmailAddress(fromUserExtensionForgot));
		message.setSubject(this.forgotSubject());
		return message;
	}

	private UriComponentsBuilder getUrlBuilder() {
		return UriComponentsBuilder.newInstance().scheme(appConfig.getWebsiteScheme())
				.host(appConfig.getWebsiteHost());
	}

	public SimpleMailMessage invitationMessage(TownshendInvitation item) {
		SimpleMailMessage message = new SimpleMailMessage();

		String url = getUrlBuilder()
				.path(appConfig.getWebsiteAcceptInvitePath())
				.queryParam(appConfig.getWebsiteInviteEmailParam(), item.getEmail())
				.queryParam(appConfig.getWebsiteInviteIdParam(), item.getId())
				.queryParam(appConfig.getWebsiteInviteCodeParam(), item.getCode())
				.build().toUriString();

		message.setTo(item.getEmail());
		message.setText(String.format("You've been invited to join %s.\n\n"
				+ "To create an account, click on the link below:\n\n"
				+ "%s\n\n", appConfig.getWebsiteHost(), url));
		message.setFrom(appConfig.getAutomatedEmailAddress());
		message.setReplyTo(appConfig.getAutomatedEmailAddress(fromUserExtensionInvite));
		message.setSubject(this.inviteSubject());
		return message;
	}

}
