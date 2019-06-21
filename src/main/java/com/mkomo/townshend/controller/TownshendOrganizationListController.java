package com.mkomo.townshend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.TownshendOrganizationList;
import com.mkomo.townshend.repository.TownshendOrganizationListRepository;

@RestController
@RequestMapping(TownshendOrganizationListController.ORGANIZATION_LIST_PATH)
public class TownshendOrganizationListController extends TownshendBaseController<TownshendOrganizationList, Long> {

	public static final String ORGANIZATION_LIST_PATH = TownshendSvcApplication.API_BASE_PATH + "/organization-lists";

	@Autowired
	private TownshendOrganizationListRepository repo;

	@Override
	protected JpaRepository<TownshendOrganizationList, Long> getRepo() {
		return this.repo;
	}

	@Override
	protected Class<TownshendOrganizationList> getResourceClass(){
		return TownshendOrganizationList.class;
	}
}
