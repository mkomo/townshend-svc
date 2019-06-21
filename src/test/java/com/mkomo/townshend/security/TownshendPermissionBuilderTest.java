package com.mkomo.townshend.security;



import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkomo.townshend.bean.TownshendInvitationRequest;
import com.mkomo.townshend.bean.TownshendOrganization;
import com.mkomo.townshend.bean.TownshendRoleUser;
import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPMethod;
import com.mkomo.townshend.security.TownshendPermissionBuilder.TPPredicateType;

public class TownshendPermissionBuilderTest {

	Long USER_ID = 12340l;
	String ROLE_NAME = "TARGET_ROLE";
	ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testRemove() throws Exception {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder();
		builder
			.addAllAdmin()
			.remove(TPMethod.DELETE);

		TownshendPermissionScheme scheme = builder.build();
		TownshendUser object = getUser();
		//Confirm that admin get is still working -- nothing is weird
		Assert.assertNull(scheme.userCanGet(object, getAdminAuth()));
		Assert.assertNotNull(scheme.userCanDelete(object, getAuth()));

	}

	@Test
	public void testAny() {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder();
		builder
			.add(TPMethod.GET, TPPredicateType.ANY);

		TownshendPermissionScheme scheme = builder.build();
		TownshendOrganization object = getOrgContainingUser();
		Assert.assertNull(scheme.userCanGet(object, getAuth()));
		Assert.assertNull(scheme.userCanGet(object, getAdminAuth()));
		Assert.assertNull(scheme.userCanGet(object, null));
	}

	@Test
	public void testAnon() {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder();
		builder
			.add(TPMethod.GET, TPPredicateType.IS_ANON);

		TownshendPermissionScheme scheme = builder.build();
		TownshendUser object = getUser();
		Assert.assertNull(scheme.userCanGet(object, null));
	}

	@Test
	public void testAdmin() {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder();
		builder
			.add(TPMethod.GET, TPPredicateType.IS_ADMIN);

		TownshendPermissionScheme scheme = builder.build();
		TownshendUser object = getUser();
		Assert.assertNull(scheme.userCanGet(object, getAdminAuth()));
		Assert.assertNotNull(scheme.userCanGet(object, getAuth()));
	}

	@Test
	public void testIdEqual() throws Exception {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder();
		builder.add(TPMethod.GET, TPPredicateType.ID_EQUAL, "id");

		TownshendPermissionScheme scheme = builder.build();
		Assert.assertNull(scheme.userCanGet(getUser(), getAuth()));
		Assert.assertNull(scheme.userCanGet(getAdminUser(), getAdminAuth()));
		Assert.assertNotNull(scheme.userCanGet(getAdminUser(), getAuth()));
		Assert.assertNotNull(scheme.userCanGet(getUser(), getAdminAuth()));

		builder = TownshendPermissionScheme.builder();
		builder.add(TPMethod.GET, TPPredicateType.ID_EQUAL, "updatedBy", "id");

		scheme = builder.build();
		Assert.assertNull(scheme.userCanGet(getTownshendInvitationRequestUpdatedByUser(), getAuth()));
		Assert.assertNotNull(scheme.userCanGet(getTownshendInvitationRequestUpdatedByUser(), getAdminAuth()));
	}

	@Test
	public void testIdIn() {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder();
		builder
			.add(TPMethod.GET, TPPredicateType.ID_IN, "users", "id");

		TownshendPermissionScheme scheme = builder.build();
		Assert.assertNull(scheme.userCanGet(getOrgContainingUser(), getAuth()));
		Assert.assertNull(scheme.userCanGet(getOrgContainingUser(), getAdminAuth()));
		Assert.assertNotNull(scheme.userCanGet(getOrgContainingUser(), getPhonyAuth()));
	}

	@Test
	public void testRolesInclude() {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder();
		builder
			.add(TPMethod.GET, TPPredicateType.ROLES_INCLUDE, ROLE_NAME);

		TownshendPermissionScheme scheme = builder.build();
		Assert.assertNull(scheme.userCanGet(new Object(), getAuth()));
		Assert.assertNotNull(scheme.userCanGet(new Object(), getAdminAuth()));
	}

	@Test
	public void testSerialize() throws Exception {
		TownshendPermissionBuilder builder = TownshendPermissionScheme.builder();
		builder
			.add(TPMethod.GET, TPPredicateType.ID_EQUAL, "id")
			.add(TPMethod.UPDATE, TPPredicateType.ID_EQUAL, "id")
			.add(TPMethod.CREATE, TPPredicateType.IS_ANON)
			.add(TPMethod.DELETE, TPPredicateType.IS_ANON);

		TownshendPermissionScheme scheme = builder.build();

		String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(scheme);
		Map<String, Object> userCanMap = mapper.readValue(s, new TypeReference<Map<String, Object>>() {});
		Assert.assertEquals(4, userCanMap.size());
		Assert.assertTrue("CREATE should be included", userCanMap.containsKey("CREATE"));
		Assert.assertTrue("items in user can map are collections", userCanMap.get("UPDATE") instanceof Collection);

	}

	private TownshendAuthentication getAuth() {
		return getAuthForUser(getUser());
	}

	private TownshendAuthentication getAdminAuth() {
		return getAuthForUser(getAdminUser());
	}

	private TownshendAuthentication getPhonyAuth() {
		return getAuthForUser(getPhonyUser());
	}

	@SuppressWarnings("serial")
	private TownshendAuthentication getAuthForUser(TownshendUser user) {

		TownshendUserDetails details = new TownshendUserDetails(user);
		TownshendClaims claims = new TownshendClaims();
		claims.put(TownshendClaims.KEY_SUB, user.getId());
		return new TownshendAuthentication(new Authentication() {

			@Override
			public String getName() {
				return null;
			}

			@Override
			public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
			}

			@Override
			public boolean isAuthenticated() {
				return true;
			}

			@Override
			public Object getPrincipal() {
				return getUser();
			}

			@Override
			public Object getDetails() {
				return details;
			}

			@Override
			public Object getCredentials() {
				return null;
			}

			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return details.getAuthorities();
			}
		}, claims);
	}

	private TownshendUser getUser() {
		TownshendUser user = new TownshendUser("testUser", "foo@bar.com", "passwd");
		user.setId(USER_ID);
		user.setRoles(Arrays.asList(new TownshendRoleUser("saflk"),
				new TownshendRoleUser("ASFJKHASKJsajfhjkfsa"),
				new TownshendRoleUser(ROLE_NAME),
				new TownshendRoleUser("abcdef")));
		return user;
	}

	private TownshendUser getPhonyUser() {
		TownshendUser user = new TownshendUser("testUser", "foo@bar.com", "passwd");
		user.setId(USER_ID + 5);
		user.setRoles(Arrays.asList());
		return user;
	}

	private TownshendUser getAdminUser() {
		TownshendUser user = new TownshendUser("testUser", "foo@bar.com", "passwd");
		user.setId(USER_ID + 99);
		user.setRoles(Arrays.asList(new TownshendRoleUser(TownshendAuthentication.ROLE_NAME_ADMIN)));
		return user;
	}

	private TownshendOrganization getOrgContainingUser() {
		TownshendOrganization org = new TownshendOrganization();
		org.setUsers(Arrays.asList(getAdminUser(), getUser()));
		return org;
	}

	private TownshendInvitationRequest getTownshendInvitationRequestUpdatedByUser() {
		TownshendInvitationRequest req = new TownshendInvitationRequest();
		req.setUpdatedBy(getUser());
		return req;
	}
}
