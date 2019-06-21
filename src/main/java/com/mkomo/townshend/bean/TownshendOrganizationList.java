package com.mkomo.townshend.bean;

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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.mkomo.townshend.bean.helper.EntityListField;
import com.mkomo.townshend.bean.helper.EntityListField.EntityListConverter;
import com.mkomo.townshend.bean.helper.OrderedRelationshipUtil;
import com.mkomo.townshend.bean.helper.TownshendAuditable;
import com.mkomo.townshend.bean.helper.TownshendOrganizationView;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)

public class TownshendOrganizationList extends TownshendAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	@NotBlank
	private String name;

	@Column()
	@NotBlank
	private String description;

	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			joinColumns=@JoinColumn(name="organization_id", referencedColumnName="id"),
			inverseJoinColumns=@JoinColumn(name="organization_list_id", referencedColumnName="id"))
	private List<TownshendOrganization> organizations;

	@Convert(converter = EntityListConverter.class)
	@JsonInclude(Include.ALWAYS) // this ensures that the value will get patched.
	@JsonProperty(access = Access.READ_ONLY)
	private EntityListField<? extends Number> organizationOrder;

	@JsonSetter("organizations")
	public void setUsers(List<TownshendOrganization> organizations) {
		this.organizations = organizations;
		this.organizationOrder = OrderedRelationshipUtil.determineOrder(this.organizations);
	}

	@JsonGetter("organizations")
	public List<TownshendOrganizationView> getOrganizationViews() {
		if (this.organizations != null) {
			this.organizations.sort(
					OrderedRelationshipUtil.getOrderComparator(this.organizationOrder)
					);
			return this.organizations.stream().map(org->new TownshendOrganizationView(org)).collect(Collectors.toList());
		} else {
			return null;
		}
	}

}
