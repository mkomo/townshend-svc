package com.mkomo.townshend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.security.TownshendPermissionBuilder.TPMethod;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPPredicateType;
import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.TownshendInvitationRequest;
import com.mkomo.townshend.repository.TownshendInvitationRequestRepository;
import com.mkomo.townshend.security.TownshendPermissionScheme;

@RestController
@RequestMapping(TownshendInvitationRequestController.PATH)
public class TownshendInvitationRequestController extends TownshendBaseController<TownshendInvitationRequest, Long> {

	public static final String PATH = TownshendSvcApplication.API_BASE_PATH + "/invitation-requests";

	@Autowired
	private TownshendInvitationRequestRepository repo;

	@Override
	protected JpaRepository<TownshendInvitationRequest, Long> getRepo() {
		return this.repo;
	}

	@Override
	protected Class<TownshendInvitationRequest> getResourceClass(){
		return TownshendInvitationRequest.class;
	}

	@Override
	protected TownshendPermissionScheme getPermissionScheme() {
		return TownshendPermissionScheme.builder()
				.addAllAdmin()
				.add(TPMethod.CREATE, TPPredicateType.IS_ANON)
				.build();
	}
}
