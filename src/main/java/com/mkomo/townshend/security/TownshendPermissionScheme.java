package com.mkomo.townshend.security;

import java.util.List;

import org.springframework.http.ResponseEntity;

public interface TownshendPermissionScheme {

	public static TownshendPermissionBuilder builder() {
		return new TownshendPermissionBuilder();
	}

	public ResponseEntity<?> userCanGet(Object entity, TownshendAuthentication u);

	public ResponseEntity<?> userCanList(List<? extends Object> list, TownshendAuthentication u);

	public ResponseEntity<?> userCanCreate(Object entity, TownshendAuthentication u);

	public ResponseEntity<?> userCanUpdate(Object entity, TownshendAuthentication u);

	public ResponseEntity<?> userCanDelete(Object entity, TownshendAuthentication u);

	/**
	 * instead of failing if an item in the list cannot be GET'd, return exactly the items in the
	 * list that can, in fact, be GET'd.
	 *
	 * @param list
	 * @param u
	 * @return
	 */
	public <T> List<T> filterList(List<T> list, TownshendAuthentication u);

}
