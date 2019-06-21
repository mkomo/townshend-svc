package com.mkomo.townshend.bean;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.mkomo.townshend.bean.helper.TownshendAuditable;
import com.mkomo.townshend.bean.helper.TownshendUserView;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class TownshendForgottenLogin extends TownshendAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String providedEmail;
	private String providedUsername;

	@JsonIgnore
	private String ipAddress;

	@OneToOne
	private TownshendUser resultantUser;

	private String code;

	private boolean used;

	@JsonSetter("emailOrUsername")
	public void setEmailOrUsername(String emailOrUsername) {
		if (emailOrUsername == null || emailOrUsername.length() == 0) {
			//DO nothing
		} else if (emailOrUsername.contains("@")) {
			this.setProvidedEmail(emailOrUsername);
		} else {
			this.setProvidedUsername(emailOrUsername);
		}
	}

	@JsonGetter("resultantUser")
	public TownshendUserView getResultantUserView() {
		return this.resultantUser == null ? null : new TownshendUserView(this.resultantUser);
	}

}
