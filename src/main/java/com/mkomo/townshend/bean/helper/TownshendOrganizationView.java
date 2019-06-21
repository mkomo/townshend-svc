package com.mkomo.townshend.bean.helper;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mkomo.townshend.bean.TownshendOrganization;
import com.mkomo.townshend.bean.TownshendRoleOrganization;
import com.mkomo.townshend.bean.helper.json.JsonSchema;

@JsonInclude(Include.NON_NULL)
public class TownshendOrganizationView {

	@JsonIgnore
	private TownshendOrganization org;

	public TownshendOrganizationView(TownshendOrganization org) {
		this.org = org;
	}

	public Long getId() {
		return org.getId();
	}

	public String getName() {
		return org.getName();
	}

	public List<TownshendRoleOrganization> getOrganizationRoles() {
		return org.getOrganizationRoles();
	}

	@JsonProperty(value = JsonSchema.SCHEMA_KEY)
	public String getSchemaName() {
		return org.getSchemaName();
	}
}
