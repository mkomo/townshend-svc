package com.mkomo.townshend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mkomo.townshend.bean.TownshendRoleUser;

public interface TownshendRoleRepository extends JpaRepository<TownshendRoleUser, Long> {

}
