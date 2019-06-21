package com.mkomo.townshend.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.TownshendOrganization;
import com.mkomo.townshend.bean.TownshendRoleOrganization;
import com.mkomo.townshend.config.TownshendAccountConfig;
import com.mkomo.townshend.repository.TownshendOrganizationRepository;
import com.mkomo.townshend.repository.TownshendOrganizationRoleRepository;
import com.mkomo.townshend.security.TownshendAuthentication;
import com.mkomo.townshend.security.TownshendPermissionBuilder;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPMethod;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPPredicateType;
import com.mkomo.townshend.security.TownshendPermissionScheme;

@RestController
@RequestMapping(TownshendOrganizationController.ORGANIZATION_PATH)
public class TownshendOrganizationController extends TownshendBaseController<TownshendOrganization, Long> {

	public static final String ORGANIZATION_PATH = TownshendSvcApplication.API_BASE_PATH + "/organizations";

	@Autowired
	private TownshendOrganizationRepository repo;

	@Autowired
	private TownshendOrganizationRoleRepository roleRepo;

	@Autowired
	private TownshendAccountConfig accountConfig;

	@Override
	protected JpaRepository<TownshendOrganization, Long> getRepo() {
		return this.repo;
	}

	@Override
	protected Class<TownshendOrganization> getResourceClass(){
		return TownshendOrganization.class;
	}

	@Override
	protected ResponseEntity<?> validateCreate(TownshendOrganization item, Map<String, Object> itemMap, TownshendAuthentication u) {
		// a user can create an org with themselves in it. an admin can create an org with anyone in it.
		if (!u.isAdmin()) {
			if (item.getUsers().size() == 0) {
				return ResponseEntity.badRequest().body("user must include themself when creating an organization");
			} else if (item.getUsers().size() > 1 || item.getUsers().get(0).getId() != u.getClaims().getSub()) {
				return ResponseEntity.badRequest().body("users may only add themselves when creating an organization");
			}

			List<Long> ids = item.getOrganizationRoles().stream().map(role -> role.getId()).collect(Collectors.toList());
			List<TownshendRoleOrganization> roles = roleRepo.findAllById(ids);
			if (roles.size() < ids.size()) {
				return ResponseEntity.badRequest().body("organization role(s) with the given id(s) not found: " + ids);
			} else if (roles.stream().anyMatch(role -> role.getPubliclyAssignable() == null || !role.getPubliclyAssignable())) {
				return ResponseEntity.badRequest().body("only publicly assignable org roles are allowed in the creation of an organization");
			}
		}
		return null;
	}

	@Override
	protected ResponseEntity<?> validateUpdate(TownshendOrganization item, Map<String, Object> updates, TownshendAuthentication u) {
		// only an admin can add user(s) to an org. users adding themselves to an org happens via user controller
		return this.validateFields(updates, u);
	}

	@Override
	public TownshendPermissionScheme getPermissionScheme() {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder()
				.addAllAdmin()
				.add(TPMethod.GET, TPPredicateType.ID_IN, "users", "id")
				.add(TPMethod.UPDATE, TPPredicateType.ID_IN, "users", "id");

		if (accountConfig.isCreateOrganization()) {
			builder.add(TPMethod.CREATE, TPPredicateType.IS_LOGGED_IN);
		}

		return builder.build();
	}

	@Override
	protected List<String> getAllowedFields(){
		return ALLOWED_FIELDS;
	}

	@Override
	protected List<String> getAllowedFieldsAdmin(){
		return ALLOWED_FIELDS_ADMIN;
	}

	public static final List<String> ALLOWED_FIELDS = Arrays.asList("name", "urlName");

	public static final List<String> ALLOWED_FIELDS_ADMIN = Arrays.asList("users", "organizationRoles");
}
