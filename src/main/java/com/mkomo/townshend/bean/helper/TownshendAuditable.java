package com.mkomo.townshend.bean.helper;

import java.util.Date;

import javax.persistence.EntityListeners;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.bean.helper.json.JsonSchema;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class TownshendAuditable {
	@CreatedDate
	private Date dateCreated;

	@LastModifiedDate
	private Date dateUpdated;

	@ManyToOne
	@CreatedBy
	@EqualsAndHashCode.Exclude
	private TownshendUser createdBy;

	@ManyToOne
	@LastModifiedBy
	@EqualsAndHashCode.Exclude
	private TownshendUser updatedBy;

	public abstract Long getId();

	@JsonGetter("createdBy")
	public TownshendUserView getCreatedByView() {
		return this.createdBy == null ? null : new TownshendUserView(this.createdBy);
	}

	@JsonGetter("updatedBy")
	public TownshendUserView getUpdatedByView() {
		return this.updatedBy == null ? null : new TownshendUserView(this.updatedBy);
	}

	@JsonProperty(value = JsonSchema.SCHEMA_KEY)
	@Transient
	public final String schemaName = JsonSchema.schemaNameFromClass(this.getClass());
}
