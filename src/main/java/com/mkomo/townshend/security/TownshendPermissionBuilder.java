package com.mkomo.townshend.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;

public class TownshendPermissionBuilder {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Logger logger = LoggerFactory.getLogger(TownshendPermissionBuilder.class);

	@Data
	@AllArgsConstructor
	public static class PermittableRequest {
		private Object requestItem;
		private TownshendAuthentication authentication;
	}

	public static enum TPMethod {
		GET, CREATE, UPDATE, DELETE;
	};

	public static interface TPPredicate {
		public boolean test(PermittableRequest req);
	}

	@Data
	@AllArgsConstructor
	public static class TPPredicateSimple implements TPPredicate {
		private TPPredicateType type;
		private String[] args;

		@Override
		public boolean test(PermittableRequest req) {
			return this.type.getPredicate(args).test(req);
		}

		public static TPPredicate of(TPPredicateType predicateType, String... args) {
			return new TPPredicateSimple(predicateType, args);
		}
	}

	@Data
	@AllArgsConstructor
	public static class TPPredicateCompound implements TPPredicate {
		private TPConnector connector;
		private TPPredicate[] predicates;

		@Override
		public boolean test(PermittableRequest req) {
			return this.connector.test(req, this.predicates);
		}

		public static TPPredicate of(TPConnector connector, TPPredicate... predicates) {
			return new TPPredicateCompound(connector, predicates);
		}
	}

	public static enum TPConnector {
		EVERY(c->c.stream().reduce(true, (a, b)->(a && b))),
		SOME(c->c.stream().reduce(false, (a, b)->(a || b)));

		private Function<List<Boolean>, Boolean> reducer;
		private TPConnector(Function<List<Boolean>, Boolean> reducer) {
			this.reducer = reducer;
		}
		public boolean test(PermittableRequest req, TPPredicate... predicates) {
			List<Boolean> booleans = Arrays.asList(predicates).stream()
					.map(predicate->predicate.test(req))
					.collect(Collectors.toList());
			return this.reducer.apply(booleans);
		}
	}

	public static enum TPPredicateType {
		ANY(path -> {
			return pm -> {
				return true;
			};
		}),
		IS_LOGGED_IN(path -> {
			return pm -> {
				return pm.getAuthentication() != null;
			};
		}),
		IS_ADMIN(path -> {
			return pm -> {
				return pm.getAuthentication() != null && pm.getAuthentication().isAdmin();
			};
		}),
		IS_ANON(path -> {
			return pm -> {
				return pm.getAuthentication() == null;
			};
		}),
		ID_EQUAL(path -> {
			return pm -> {
				try {
					JsonNode item = MAPPER.valueToTree(pm.getRequestItem());
					for (String fieldName : path) {
						item = getProperty(item, fieldName);
					}
					return item != null && pm.getAuthentication() != null &&
							pm.getAuthentication().getClaims().getSub().equals(item.asLong());
				} catch (Exception e) {
					logger.debug("error finding property for predicate ID_EQUAL", e);
					return false;
				}
			};
		}),
		ID_IN(path -> {
			return pm -> {
				try {
					JsonNode item = MAPPER.valueToTree(pm.getRequestItem());
					return collectionSafeMatch(item, path,
							pm.getAuthentication().getClaims().getSub(),
							i -> i.asLong());
				} catch (Exception e) {
					logger.debug("error finding property for predicate ID_IN", e);
					return false;
				}
			};
		}),
		ROLES_INCLUDE(path -> {
			return pm -> {
				return path.length == 1 && pm.getAuthentication() != null &&
						pm.getAuthentication().getAuthorities()
							.stream().anyMatch(auth->auth.getAuthority().equals(path[0]));
			};
		}),
		TARGET_TRUE(path -> {
			return pm -> {
				try {
					JsonNode item = MAPPER.valueToTree(pm.getRequestItem());
					for (String fieldName : path) {
						item = getProperty(item, fieldName);
					}
					return item != null && item.isBoolean() && item.asBoolean();
				} catch (Exception e) {
					logger.debug("error finding property for predicate TARGET_TRUE", e);
					return false;
				}
			};
		}),
		TARGET_EMPTY(path -> {
			return pm -> {
				try {
					JsonNode item = MAPPER.valueToTree(pm.getRequestItem());
					for (String fieldName : path) {
						item = getProperty(item, fieldName);
					}
					return item == null || !item.elements().hasNext();
				} catch (Exception e) {
					logger.debug("error finding property for predicate TARGET_TRUE", e);
					return false;
				}
			};
		});

		private Function<String[], Predicate<PermittableRequest>> predicateFunction;

		private TPPredicateType(Function<String[], Predicate<PermittableRequest>> predicateFunction) {
			this.predicateFunction = predicateFunction;
		}

		public Predicate<PermittableRequest> getPredicate(final String... path) {
			return predicateFunction.apply(path);
		}
	}

	private Map<TPMethod, List<TPPredicate>> permissionMap = new LinkedHashMap<>();

	@Data
	public static class TownshendPermissionBuilderScheme implements TownshendPermissionScheme {

		@JsonValue
		private final Map<TPMethod, List<TPPredicate>> permissionMap;

		public TownshendPermissionBuilderScheme(Map<TPMethod, List<TPPredicate>> permissionMap) {
			this.permissionMap = permissionMap;
		}

		private ResponseEntity<?> userCan(TPMethod method, Object entity, TownshendAuthentication u) {
			String username = u != null ? u.getName() : "*anon*";
			if (permissionMap.get(method) != null) {
				PermittableRequest req = new PermittableRequest(entity, u);
				for (TPPredicate pred : permissionMap.get(method)) {
					if (pred.test(req)) {
						logger.info(username + " can " + method + " " + entity);
						return null;
					} else {
						logger.info(username + " failed " + pred + " for " + entity);
					}
				}
			}
			logger.info(username + " can not " + method + " " + entity);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		@Override
		public ResponseEntity<?> userCanGet(Object entity, TownshendAuthentication u) {
			return this.userCan(TPMethod.GET, entity, u);
		}

		@Override
		public ResponseEntity<?> userCanList(List<?> list, TownshendAuthentication u) {

			for (Object item : list) {
				ResponseEntity<?> resp = this.userCan(TPMethod.GET, item, u);
				if (resp != null) {
					return resp;
				}
			}
			return null;
		}

		@Override
		public <T> List<T> filterList(List<T> list, TownshendAuthentication u) {
			List<T> outputList = new ArrayList<>();
			for (T item : list) {
				ResponseEntity<?> resp = this.userCan(TPMethod.GET, item, u);
				if (resp == null) {
					outputList.add(item);
				}
			}
			return outputList;
		}

		@Override
		public ResponseEntity<?> userCanCreate(Object entity, TownshendAuthentication u) {
			return this.userCan(TPMethod.CREATE, entity, u);
		}

		@Override
		public ResponseEntity<?> userCanUpdate(Object entity, TownshendAuthentication u) {
			return this.userCan(TPMethod.UPDATE, entity, u);
		}

		@Override
		public ResponseEntity<?> userCanDelete(Object entity, TownshendAuthentication u) {
			return this.userCan(TPMethod.DELETE, entity, u);
		}
	}

	public TownshendPermissionScheme build() {
		return new TownshendPermissionBuilderScheme(this.permissionMap);
	}

	public static JsonNode getProperty(JsonNode item, String fieldName) {
		return item.get(fieldName);
	}

	public TownshendPermissionBuilder add(TPMethod method, TPPredicate predicate) {
		if (permissionMap.get(method) == null) {
			permissionMap.put(method, new ArrayList<>());
		}
		permissionMap.get(method).add(predicate);
		return this;
	}

	public TownshendPermissionBuilder add(TPMethod method, TPPredicateType predicateType, String... args) {
		return this.add(method, new TPPredicateSimple(predicateType, args));
	}

	public TownshendPermissionBuilder addAll(TPPredicate predicate) {
		for (TPMethod method : TPMethod.values()) {
			this.add(method, predicate);
		}
		return this;
	}

	public TownshendPermissionBuilder addAll(TPPredicateType predicateType, String... args) {
		TPPredicate pred = new TPPredicateSimple(predicateType, args);
		return this.addAll(pred);
	}

	public TownshendPermissionBuilder addAllAdmin() {
		for (TPMethod method : TPMethod.values()) {
			this.add(method, TPPredicateType.IS_ADMIN, new String[0]);
		}
		return this;
	}

	public TownshendPermissionBuilder addAllLoggedIn() {
		for (TPMethod method : TPMethod.values()) {
			this.add(method, TPPredicateType.IS_LOGGED_IN);
		}
		return this;
	}
	public TownshendPermissionBuilder remove(TPMethod method) {
		permissionMap.remove(method);
		return this;
	}

	public static boolean collectionSafeMatch(JsonNode item, String[] path, Object matchValue, Function<JsonNode, Object> map) {
		if (path.length == 0) {
			return matchValue.equals(map.apply(item));
		} else {
			String fieldName = path[0];
			path = Arrays.copyOfRange(path, 1, path.length);
			if (item.isArray()) {
				for (JsonNode val : item) {
					val = getProperty(val, fieldName);
					if (collectionSafeMatch(val, path, matchValue, map)) {
						return true;
					}
				}
				return false;
			} else {
				item = getProperty(item, fieldName);
				return collectionSafeMatch(item, path, matchValue, map);
			}
		}
	}
}
