package com.mkomo.townshend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.TownshendRoleUser;
import com.mkomo.townshend.repository.TownshendRoleRepository;

@RestController
@RequestMapping(TownshendRoleController.ROLE_PATH)
public class TownshendRoleController extends TownshendBaseController<TownshendRoleUser, Long> {

	public static final String ROLE_PATH = TownshendSvcApplication.API_BASE_PATH + "/roles";

	@Autowired
	private TownshendRoleRepository repo;

	@Override
	protected JpaRepository<TownshendRoleUser, Long> getRepo() {
		return this.repo;
	}

	@Override
	protected Class<TownshendRoleUser> getResourceClass(){
		return TownshendRoleUser.class;
	}
}
