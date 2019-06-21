package com.mkomo.townshend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mkomo.townshend.bean.TownshendOrganization;

public interface TownshendOrganizationRepository extends JpaRepository<TownshendOrganization, Long> {

}
