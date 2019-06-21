package com.mkomo.townshend.bean.helper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.bean.helper.json.JsonSchema;

@JsonInclude(Include.NON_NULL)
public class TownshendUserView {

	@JsonIgnore
	private TownshendUser user;

	public TownshendUserView(TownshendUser user) {
		this.user = user;
	}

	public Long getId() {
		return user.getId();
	}

	public String getUsername() {
		return user.getUsername();
	}

	@JsonProperty(value = JsonSchema.SCHEMA_KEY)
	public String getSchemaName() {
		return user.getSchemaName();
	}
}
