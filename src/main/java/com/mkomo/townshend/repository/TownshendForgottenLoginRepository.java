package com.mkomo.townshend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mkomo.townshend.bean.TownshendForgottenLogin;

public interface TownshendForgottenLoginRepository extends JpaRepository<TownshendForgottenLogin, Long> {

}
