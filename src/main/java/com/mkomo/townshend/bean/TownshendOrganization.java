package com.mkomo.townshend.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Convert;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.mkomo.townshend.bean.helper.EntityListField;
import com.mkomo.townshend.bean.helper.OrderedRelationshipUtil;
import com.mkomo.townshend.bean.helper.TownshendAuditable;
import com.mkomo.townshend.bean.helper.TownshendUserView;
import com.mkomo.townshend.bean.helper.EntityListField.EntityListConverter;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class TownshendOrganization extends TownshendAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	@NotBlank
	private String name;

	@Column(unique = true)
	@Pattern(regexp="[a-zA-Z][a-zA-Z0-9]*", message="must start with a letter and contain "
			+ "only letters and numbers.")
	private String urlName;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity=TownshendRoleOrganization.class)
	@JoinTable(
			joinColumns=@JoinColumn(name="organization_role_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="organization_id", referencedColumnName="id"))
	private List<TownshendRoleOrganization> organizationRoles;

//	@Transient
//	private boolean shouldFetchUsers;

	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			joinColumns=@JoinColumn(name="user_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="organization_id", referencedColumnName="id"))
	private List<TownshendUser> users;

	@Convert(converter = EntityListConverter.class)
	@JsonInclude(Include.ALWAYS) // this ensures that the value will get patched.
	@JsonProperty(access = Access.READ_ONLY)
	private EntityListField<? extends Number> userOrder;

	@JsonSetter("users")
	public void setUsers(List<TownshendUser> users) {
		this.users = users;
		this.userOrder = OrderedRelationshipUtil.determineOrder(this.users);
	}

	@JsonGetter("users")
	public List<TownshendUserView> getUserViews() {
		if (this.users != null) {
			this.users.sort(
					OrderedRelationshipUtil.getOrderComparator(this.userOrder)
					);
			return this.users.stream().map(user->new TownshendUserView(user)).collect(Collectors.toList());
		} else {
			return null;
		}
	}

	public TownshendOrganization(TownshendRoleOrganization organizationRole, String name) {
		this.organizationRoles = new ArrayList<>();
		this.organizationRoles.add(organizationRole);
		this.name = name;
	}

}
