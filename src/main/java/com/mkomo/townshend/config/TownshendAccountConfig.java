package com.mkomo.townshend.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TownshendAccountConfig {
	private boolean createAccountPublic;
	private boolean createAccountWithInvite;
	private boolean requestInvite;
	private boolean usernameChange;
	private boolean emailChange;
	private boolean createOrganization;
	private boolean inviteNewUserToOrganization;
	//TODO	private boolean inviteExistingUserToOrganization;

}
