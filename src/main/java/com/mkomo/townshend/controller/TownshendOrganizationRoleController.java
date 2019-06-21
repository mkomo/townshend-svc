package com.mkomo.townshend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.TownshendRoleOrganization;
import com.mkomo.townshend.repository.TownshendOrganizationRoleRepository;

@RestController
@RequestMapping(TownshendOrganizationRoleController.ROLE_PATH)
public class TownshendOrganizationRoleController extends TownshendBaseController<TownshendRoleOrganization, Long> {

	public static final String ROLE_PATH = TownshendSvcApplication.API_BASE_PATH + "/organization-roles";

	@Autowired
	private TownshendOrganizationRoleRepository repo;

	@Override
	protected JpaRepository<TownshendRoleOrganization, Long> getRepo() {
		return this.repo;
	}

	@Override
	protected Class<TownshendRoleOrganization> getResourceClass(){
		return TownshendRoleOrganization.class;
	}
}
