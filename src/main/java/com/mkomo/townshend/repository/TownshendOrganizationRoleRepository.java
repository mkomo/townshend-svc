package com.mkomo.townshend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mkomo.townshend.bean.TownshendRoleOrganization;

public interface TownshendOrganizationRoleRepository extends JpaRepository<TownshendRoleOrganization, Long> {

}
