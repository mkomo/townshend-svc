package com.mkomo.townshend.bean;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.mkomo.townshend.bean.helper.TownshendAuditable;
import com.mkomo.townshend.bean.helper.TownshendOrganizationView;
import com.mkomo.townshend.bean.helper.TownshendUserView;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class TownshendInvitation extends TownshendAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	@NotBlank
	private String email;
	@ManyToOne
	private TownshendRoleUser role;
	@ManyToOne
	private TownshendOrganization organization;

	@ManyToOne
	private TownshendUser inviter;
	@ManyToOne
	private TownshendUser resultantUser;

	@Transient
	private TownshendInvitationRequest invitationRequest;

	private Date inviteDate;
	private String code;
	private boolean sent;
	private boolean opened;

	public TownshendInvitation(String email,
			TownshendRoleUser role,
			TownshendOrganization organization,
			TownshendUser inviter) {
		this.email = email;
		this.role = role;
		this.organization = organization;
		this.inviter = inviter;
	}

	@JsonGetter("organization")
	public TownshendOrganizationView getOrganizationView() {
		return this.organization == null
				? null
				: new TownshendOrganizationView(this.organization);
	}

	@JsonGetter("inviter")
	public TownshendUserView getInviterView() {
		return this.inviter == null ? null : new TownshendUserView(this.inviter);
	}

	@JsonGetter("resultantUser")
	public TownshendUserView getResultantUserView() {
		return this.resultantUser == null ? null : new TownshendUserView(this.resultantUser);
	}
}
