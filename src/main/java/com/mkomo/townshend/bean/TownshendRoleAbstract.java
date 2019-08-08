package com.mkomo.townshend.bean;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mkomo.townshend.bean.helper.TownshendAuditable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@JsonInclude(Include.NON_NULL)
public class TownshendRoleAbstract extends TownshendAuditable {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private Long id;

	@Column(unique = true, nullable = false)
	@Pattern(regexp="[A-Z][_0-9A-Z]*", message="must start with an UPPERCASE letter and contain "
			+ "only UPPERCASE letters, numbers and '_'")
	@NotBlank
	private String roleName;

	private Boolean publiclyAssignable;

	public TownshendRoleAbstract(String roleName) {
		this.roleName = roleName;
	}
}
