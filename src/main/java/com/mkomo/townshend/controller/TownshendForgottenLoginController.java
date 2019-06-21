package com.mkomo.townshend.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.TownshendForgottenLogin;
import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.bean.helper.PasswordResetRequest;
import com.mkomo.townshend.config.SecurityConfig;
import com.mkomo.townshend.mail.TownshendMailService;
import com.mkomo.townshend.repository.TownshendForgottenLoginRepository;
import com.mkomo.townshend.repository.TownshendUserRepository;
import com.mkomo.townshend.security.TownshendAuthentication;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPMethod;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPPredicateType;
import com.mkomo.townshend.security.TownshendPermissionScheme;
import com.mkomo.townshend.security.TownshendValidationCodeProvider;

@RestController
@RequestMapping(TownshendForgottenLoginController.PATH)
public class TownshendForgottenLoginController extends TownshendBaseController<TownshendForgottenLogin, Long> {

	public static final String PATH = TownshendSvcApplication.API_BASE_PATH + "/forgotten-logins";
	public static final String PATH_RESET_FRAGMENT = "reset";
	public static final String PATH_RESET = PATH + "/" + PATH_RESET_FRAGMENT;

	//TODO get this from config (with default)
	private long maxResetAge = 1000 * 60 * 60; // one hour

	@Autowired
	private TownshendForgottenLoginRepository repo;
	@Autowired
	private TownshendUserRepository userRepo;
	@Autowired
	private TownshendValidationCodeProvider codeProvider;
	@Autowired
	private TownshendMailService mailService;
	@Autowired
	private SecurityConfig securityConfig;

	@Override
	protected JpaRepository<TownshendForgottenLogin, Long> getRepo() {
		return this.repo;
	}

	@Override
	protected Class<TownshendForgottenLogin> getResourceClass(){
		return TownshendForgottenLogin.class;
	}

	@Override
	protected TownshendPermissionScheme getPermissionScheme() {
		return TownshendPermissionScheme.builder()
				.addAllAdmin()
				.add(TPMethod.CREATE, TPPredicateType.IS_ANON)
				.build();
	}

	@Override
	protected ResponseEntity<?> validateCreate(TownshendForgottenLogin item, Map<String, Object> itemMap) {
		if (item.getProvidedUsername() != null) {
			Optional<TownshendUser> user = userRepo.findByUsername(item.getProvidedUsername());
			if (user.isPresent()) {
				item.setResultantUser(user.get());
				return null;
			} else {
				return ResponseEntity.badRequest().body("Could not find username.");
			}
		} else if (item.getProvidedEmail() != null) {
			Optional<TownshendUser> user = userRepo.findByEmail(item.getProvidedEmail());
			if (user.isPresent()) {
				item.setResultantUser(user.get());
				return null;
			} else {
				return ResponseEntity.badRequest().body("Could not find email.");
			}
		} else {
			return ResponseEntity.badRequest().body("must specify username or email");
		}
	}

	@Override
	protected void prepareForCreate(TownshendForgottenLogin tfl) {
		tfl.setCode(codeProvider.generateCode());
	}

	@Override
	protected TownshendForgottenLogin onCreateSuccess(TownshendForgottenLogin savedItem, TownshendForgottenLogin originalItem) {
		SimpleMailMessage message = mailService.passwordResetMessage(savedItem);
		mailService.sendSimpleMessage(message);
		return savedItem;
	}



	@PostMapping(path=PATH_RESET_FRAGMENT)
	public ResponseEntity<?> reset(@RequestBody PasswordResetRequest reset, TownshendAuthentication u) {
		Optional<TownshendForgottenLogin> o = repo.findById(reset.getId());
		if (!o.isPresent()) {
			return ResponseEntity.badRequest().body("invalid reset id");
		}
		TownshendForgottenLogin item = o.get();
		long age = System.currentTimeMillis() - item.getDateCreated().getTime();
		if (age > this.maxResetAge) {
			return ResponseEntity.badRequest().body("reset code has expired");
		} else if (item.isUsed() == true) {
			return ResponseEntity.badRequest().body("reset code has already been used");
		} else if (!item.getCode().equals(reset.getCode())) {
			return ResponseEntity.badRequest().body("invalid reset code");
		} else if (reset.getPassword() == null) {
			return ResponseEntity.badRequest().body("must include password");
		} else {
			item.setUsed(true);
			item = repo.save(item);
			TownshendUser user = item.getResultantUser();
			user.setPassword(securityConfig.passwordEncoder().encode(reset.getPassword()));
			user = userRepo.save(user);
			return ResponseEntity.ok().body(item);
		}
	}
}
