package com.mkomo.townshend.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.mkomo.townshend.bean.helper.TownshendAuditable;
import com.mkomo.townshend.bean.helper.TownshendOrganizationView;
import com.mkomo.townshend.config.TownshendFieldConfig;
import com.mkomo.townshend.security.TownshendClaims;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class TownshendUser extends TownshendAuditable {

	public static final String PASSWORD_KEY = "password";


	public TownshendUser(String username, String email, String password) {
		this.username = username;
		this.email = email;
		this.password = password;
	}

	public TownshendUser(TownshendClaims claims) {
		this.id = claims.getSub();
		this.username = claims.getUserName();
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	@Pattern(regexp="[a-zA-Z][_.a-zA-Z0-9]*", message="must start with a letter and contain "
			+ "only letters, underscores, dots, and numbers.")
	@NotBlank
	@Size(max = TownshendFieldConfig.USER_MAX_USERNAME_LENGTH, min = TownshendFieldConfig.USER_MIN_USERNAME_LENGTH)
	private String username;

	@Column(unique = true)
	@Email
	private String email;

	@NotBlank
	@Size(max = TownshendFieldConfig.USER_MAX_PASSWORD_LENGTH, min = TownshendFieldConfig.USER_MIN_PASSWORD_LENGTH)
	@JsonProperty(access = Access.WRITE_ONLY) @ToString.Exclude //avoid security issues
	private String password;

	@Transient
	@ToString.Exclude
	private TownshendInvitation invitation;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			joinColumns=@JoinColumn(name="role_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="user_id", referencedColumnName="id"))
	private List<TownshendRoleUser> roles;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "users", cascade = CascadeType.ALL)
	private List<TownshendOrganization> organizations;

	@JsonGetter("organizations")
	public List<TownshendOrganizationView> getOrganizationView() {
		return this.organizations == null
				? null
				: this.organizations.stream().map(org->new TownshendOrganizationView(org)).collect(Collectors.toList());
	}


	public void addRole(TownshendRoleUser townshendRole) {
		if (this.getRoles() == null) {
			this.setRoles(new ArrayList<>());
		}
		this.roles.add(townshendRole);
	}

	public void addOrganization(TownshendOrganization org) {
		if (this.getOrganizations() == null) {
			this.setOrganizations(new ArrayList<>());
		}
		this.organizations.add(org);
	}

	/**
	 * TODO
	 * other login methods.
	 * identity confirmation to prevent at-will creation of accounts (probably phone, facebook, linkedin, github)
	 * invitation system
	 * frontend alerts for successful acct creation, failed acct creation, successful + failed login
	 * field validation frontend and graceful validation backend (no 500s)
	 * hide login and create based on configuration and state (logged in => hide both, invitation only =>
	 * hide create and add an option to get on a list
	 * password reset
	 * change password; if config allows, user can change username too
	 * 2-factor auth/mfa
	 * figure out how to allow validation overrides
	 */

	//
}
