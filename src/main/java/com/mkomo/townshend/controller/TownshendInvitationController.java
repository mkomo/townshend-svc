package com.mkomo.townshend.controller;

import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.TownshendInvitation;
import com.mkomo.townshend.bean.TownshendInvitationRequest;
import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.config.TownshendAccountConfig;
import com.mkomo.townshend.mail.TownshendMailService;
import com.mkomo.townshend.repository.TownshendInvitationRepository;
import com.mkomo.townshend.repository.TownshendInvitationRequestRepository;
import com.mkomo.townshend.repository.TownshendOrganizationRepository;
import com.mkomo.townshend.repository.TownshendUserRepository;
import com.mkomo.townshend.security.TownshendAuthentication;
import com.mkomo.townshend.security.TownshendPermissionBuilder;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPMethod;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPPredicateType;
import com.mkomo.townshend.security.TownshendPermissionScheme;
import com.mkomo.townshend.security.TownshendValidationCodeProvider;

@RestController
@RequestMapping(TownshendInvitationController.PATH)
public class TownshendInvitationController extends TownshendBaseController<TownshendInvitation, Long> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static final String PATH = TownshendSvcApplication.API_BASE_PATH + "/invitations";

	@Autowired
	private TownshendInvitationRepository repo;

	@Autowired
	private TownshendInvitationRequestRepository inviteRequestRepo;

	@Autowired
	private TownshendUserRepository userRepo;

	@Autowired
	private TownshendOrganizationRepository orgRepo;

	@Autowired
	private TownshendValidationCodeProvider codeProvider;

	@Autowired
	private TownshendMailService mailService;

	@Autowired
	private TownshendAccountConfig accountConfig;

	@Override
	protected JpaRepository<TownshendInvitation, Long> getRepo() {
		return this.repo;
	}

	@Override
	protected Class<TownshendInvitation> getResourceClass(){
		return TownshendInvitation.class;
	}

	@Override
	protected void prepareForCreate(TownshendInvitation invite) {
		if (invite.getOrganization() != null) {
			invite.setOrganization(orgRepo.getOne(invite.getOrganization().getId()));
		}
		invite.setCode(codeProvider.generateCode());
		invite.setInviteDate(new Date());
	}

	@Override
	protected TownshendInvitation onCreateSuccess(TownshendInvitation savedItem, TownshendInvitation originalItem) {
		SimpleMailMessage message = mailService.invitationMessage(savedItem);
		mailService.sendSimpleMessage(message);

		if (originalItem.getInvitationRequest() != null && originalItem.getInvitationRequest().getId() != null) {
			TownshendInvitationRequest req = inviteRequestRepo.getOne(originalItem.getInvitationRequest().getId());
			req.setResultantInvitation(savedItem);
			inviteRequestRepo.save(req);
		}
		return savedItem;
	}

	@Override
	protected TownshendPermissionScheme getPermissionScheme() {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder()
				.addAllAdmin();

		if (accountConfig.isInviteNewUserToOrganization()) {
			builder
				.add(TPMethod.GET, TPPredicateType.ID_IN, "organization", "users", "id")
				.add(TPMethod.CREATE, TPPredicateType.ID_IN, "organization", "users", "id");
		}

		return builder.build();
	}

	@RequestMapping(path = "{id}", params = "code", method = RequestMethod.GET)
	public ResponseEntity<?> get(@PathVariable Long id, @RequestParam String code,
			TownshendAuthentication u) {
		if (code == null) {
			return super.get(id, u);
		} else {
			return this.validateInvitation(id, code);
		}
	}



	public ResponseEntity<?> validateInvitation(Long invitationId, String code){
		Optional<TownshendInvitation> i = getRepo().findById(invitationId);
		if (!i.isPresent()) {
			return ResponseEntity.badRequest().body("Could not find invitation");
		} else {
			TownshendInvitation invite = i.get();
			if (invite.getResultantUser() != null) {
				return ResponseEntity.badRequest().body("Invitation has already been used");
			} else if (!invite.getCode().equals(code)) {
				return ResponseEntity.badRequest().body("Invite code is not valid");
			} else {
				return ResponseEntity.ok(invite);
			}
		}
	}

	public void consumeInvitation(TownshendUser user, TownshendInvitation invite) {
		invite.setResultantUser(user);
		invite = getRepo().save(invite);

		if (invite.getOrganization() != null) {
			logger.info("invite adding org {}\nsaved {}", invite.getOrganization(), user);
			user.addOrganization(invite.getOrganization());
			invite.getOrganization().getUsers().add(user);
			orgRepo.save(invite.getOrganization());
		}
		if (invite.getRole() != null) {
			logger.info("invite adding role {}\nsaved {}", invite.getRole(), user);
			user.addRole(invite.getRole());
			user = userRepo.save(user);
		}
	}
}
