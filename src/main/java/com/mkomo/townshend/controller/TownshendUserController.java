package com.mkomo.townshend.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.TownshendInvitation;
import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.bean.helper.TownshendEntityError;
import com.mkomo.townshend.config.SecurityConfig;
import com.mkomo.townshend.config.TownshendAccountConfig;
import com.mkomo.townshend.repository.TownshendUserRepository;
import com.mkomo.townshend.security.TownshendAuthentication;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPMethod;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPPredicateType;
import com.mkomo.townshend.security.TownshendPermissionScheme;

@RestController
@RequestMapping(path = TownshendUserController.USER_PATH)
public class TownshendUserController extends TownshendBaseController<TownshendUser, Long> {

	public static final List<String> ALLOWED_FIELDS = Arrays.asList("username", "email", "invitation", TownshendUser.PASSWORD_KEY);

	public static final List<String> ALLOWED_FIELDS_ADMIN = Arrays.asList("roles", "organizations");

	public static final String USER_PATH = TownshendSvcApplication.API_BASE_PATH + "/users";

	private static Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	@Autowired
	private TownshendUserRepository repo;

	@Autowired
	private TownshendInvitationController inviteController;
	@Autowired
	private SecurityConfig securityConfig;
	@Autowired
	private TownshendAccountConfig accountConfig;

	private Logger logger = LoggerFactory.getLogger(this.getClass());


	@Override
	protected TownshendUserRepository getRepo() {
		return this.repo;
	}

	@Override
	protected Class<TownshendUser> getResourceClass() {
		return TownshendUser.class;
	}

	@Override
	protected TownshendPermissionScheme getPermissionScheme() {
		return TownshendPermissionScheme.builder()
				.addAllAdmin()
				.add(TPMethod.GET, TPPredicateType.ID_EQUAL, "id")
				.add(TPMethod.UPDATE, TPPredicateType.ID_EQUAL, "id")
				.add(TPMethod.CREATE, TPPredicateType.IS_ANON)
				.remove(TPMethod.DELETE)
				.build();
	}

	@Override
	protected List<String> getAllowedFields(){
		return ALLOWED_FIELDS;
	}

	@Override
	protected List<String> getAllowedFieldsAdmin(){
		return ALLOWED_FIELDS_ADMIN;
	}

	@Override
	protected ResponseEntity<?> validateCreate(TownshendUser user, Map<String, Object> itemMap, TownshendAuthentication u) {

		if (user.getInvitation() == null || user.getInvitation().getId() == null) {
			if (!this.accountConfig.isCreateAccountPublic() && (u == null || !u.isAdmin())) {
				return ResponseEntity.badRequest().body("account creation by invitation only.");
			}
		} else {
			if (!this.accountConfig.isCreateAccountWithInvite() && (u == null || !u.isAdmin())){
				return ResponseEntity.badRequest().body("account creation is not allowed at this time.");
			}

			ResponseEntity<?> resp = inviteController.validateInvitation(user.getInvitation().getId(),
					user.getInvitation().getCode());
			if (resp.getStatusCode().isError()) {
				return resp;
			} else {
				user.setInvitation((TownshendInvitation) resp.getBody());
			}
		}

		if (user.getUsername() == null) {
			return ResponseEntity.badRequest().body(
					TownshendEntityError.fieldError("username", "Username must be specified to create a new user"));
		} else {
			Set<ConstraintViolation<TownshendUser>> violations = VALIDATOR.validate(user);
			if (!violations.isEmpty()) {
				return ResponseEntity.badRequest().body(TownshendEntityError.of(violations));
			}
			ResponseEntity<?> resp = this.validateUnique(user.getUsername(), user.getEmail());
			if (resp != null) {
				return resp;
			}
		}
		return this.validateFields(itemMap, u);
	}

	@Override
	protected ResponseEntity<?> validateUpdate(TownshendUser user, Map<String, Object> updates, TownshendAuthentication u) {
		if (updates.containsKey("username") && !this.accountConfig.isUsernameChange()) {
			return ResponseEntity.badRequest().body("app does not allow changes to username");
		} else if (updates.containsKey("email") && !this.accountConfig.isEmailChange()) {
			return ResponseEntity.badRequest().body("app does not allow changes to email");
		} else {
			if (updates.containsKey(TownshendUser.PASSWORD_KEY)) {
				Set<ConstraintViolation<TownshendUser>> violations =
						VALIDATOR.validateValue(TownshendUser.class, TownshendUser.PASSWORD_KEY,
								updates.get(TownshendUser.PASSWORD_KEY));
				if (!violations.isEmpty()) {
					return ResponseEntity.badRequest().body(TownshendEntityError.of(violations));
				}
			}
			ResponseEntity<?> resp = this.validateUnique((String)updates.get("username"), (String)updates.get("email"));
			if (resp != null) {
				return resp;
			}
		}
		return this.validateFields(updates, u);
	}

	protected ResponseEntity<?> validateUnique(String username, String email) {
		if (username != null && getRepo().existsByUsername(username)) {
			return ResponseEntity.badRequest().body(
					TownshendEntityError.fieldError("username", "username '" + username + "' is already used"));
		} else if (email != null && getRepo().existsByEmail(email)) {
			return ResponseEntity.badRequest().body(
					TownshendEntityError.fieldError("email", "A user has already been created with that email"));
		} else {
			return null;
		}
	}

	@Override
	protected void prepareForCreate(TownshendUser user) {
		if (user.getPassword() != null) {
			user.setPassword(securityConfig.passwordEncoder().encode(user.getPassword()));
		} else {
			logger.warn("user being created with null password: {}", user.getUsername());
		}
	}

	@Override
	protected TownshendUser onCreateSuccess(TownshendUser saved, TownshendUser original) {
		if (original.getInvitation() != null && original.getInvitation().getId() != null) {
			inviteController.consumeInvitation(saved, original.getInvitation());
		}
		return saved;
	}

	@Override
	protected void prepareForUpdate(TownshendUser user, Map<String, Object> updates) {
		if (updates.containsKey(TownshendUser.PASSWORD_KEY)) {
			user.setPassword(securityConfig.passwordEncoder().encode(user.getPassword()));
		}
	}

	@RequestMapping(params = "username", method = RequestMethod.HEAD)
	public ResponseEntity<?> isUsernameUsed(@RequestParam String username) {
		if (getRepo().existsByUsername(username)) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping(params = "email", method = RequestMethod.HEAD)
	public ResponseEntity<?> isEmailUsed(@RequestParam String email) {
		if (getRepo().existsByEmail(email)) {
			return ResponseEntity.ok().build();
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping(path = "name/{username}", method = RequestMethod.GET)
	public ResponseEntity<?> getByUsername(@PathVariable String username,
			TownshendAuthentication u) {
		Optional<TownshendUser> o = getRepo().findByUsername(username);

		return handleGet(o, u);
	}

	@RequestMapping(path = "{id}/invitation", method = RequestMethod.POST)
	public ResponseEntity<?> postInvitation(@PathVariable Long id, @RequestBody TownshendInvitation requestInvitation,
			TownshendAuthentication u) {

		Optional<TownshendUser> o = getRepo().findById(id);
		if (!o.isPresent()) {
			return ResponseEntity.notFound().build();
		} else {
			ResponseEntity<?> resp = userCanUpdate(o.get(), u);
			if (resp != null) {
				return resp;
			}
			resp = inviteController.validateInvitation(requestInvitation.getId(), requestInvitation.getCode());
			if (resp.getStatusCode().isError()) {
				return resp;
			}
			TownshendUser user = o.get();

			inviteController.consumeInvitation(user, (TownshendInvitation) resp.getBody());

			return ResponseEntity.ok(user);
		}
	}

}
