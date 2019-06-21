package com.mkomo.townshend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mkomo.townshend.bean.TownshendUser;

public interface TownshendUserRepository extends JpaRepository<TownshendUser, Long> {

	public boolean existsByUsername(String username);

	public boolean existsByEmail(String email);

	public Optional<TownshendUser> findByUsername(String username);

	public Optional<TownshendUser> findByEmail(String email);
}
