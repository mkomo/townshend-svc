package com.mkomo.townshend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mkomo.townshend.bean.TownshendInvitationRequest;

public interface TownshendInvitationRequestRepository extends JpaRepository<TownshendInvitationRequest, Long> {

}
