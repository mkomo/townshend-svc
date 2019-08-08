package com.mkomo.townshend.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mkomo.townshend.bean.helper.EntitySchemaGenerator;
import com.mkomo.townshend.bean.helper.json.JsonSchema;
import com.mkomo.townshend.security.TownshendAuthentication;
import com.mkomo.townshend.security.TownshendPermissionScheme;

@RestController
public abstract class TownshendBaseController<T,ID> {

	/**
	 * Use autowired mapper b/c it will format date in the application-wide standard way
	 */
	@Autowired
	private ObjectMapper MAPPER;

	private static final String FIELD_FILTER_SEPARATOR = ",";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private TownshendConfigController config;

	protected abstract JpaRepository<T,ID> getRepo();

	protected abstract Class<T> getResourceClass();

	protected List<String> getAllowedFields(){
		return null;
	}

	protected List<String> getAllowedFieldsAdmin(){
		return null;
	}

	protected ResponseEntity<?> validateFields(Map<String, Object> itemMap, TownshendAuthentication u) {
		List<String> allowedFields = getAllowedFields();
		List<String> allowedFieldsAdmin = getAllowedFieldsAdmin();

		for (String key : itemMap.keySet()) {
			if (allowedFieldsAdmin != null && allowedFieldsAdmin.contains(key)) {
				if (!u.isAdmin()) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body("this endpoint does not allow non-admin changes to: " + key);
				}
			} else if (allowedFields != null && !allowedFields.contains(key)) {
				return ResponseEntity.badRequest().body("this endpoint does not allow changes to: " + key);
			}
		}
		return null;
	}

	protected ResponseEntity<?> validateCreate(T item, Map<String, Object> itemMap, TownshendAuthentication u) {
		return this.validateCreate(item, itemMap);
	}

	protected ResponseEntity<?> validateCreate(T item, Map<String, Object> itemMap) {
		return null;
	}

	protected ResponseEntity<?> validateUpdate(T item, Map<String, Object> updates, TownshendAuthentication u) {
		return this.validateUpdate(item, updates);
	}

	protected ResponseEntity<?> validateUpdate(T item, Map<String, Object> updates) {
		return null;
	}

	protected TownshendPermissionScheme getPermissionScheme() {
		return TownshendPermissionScheme.builder().addAllAdmin().build();
	}

	protected final ResponseEntity<?> userCanGet(T entity, TownshendAuthentication u) {
		logger.info("checking user	GET {}	{}", entity.getClass().getSimpleName(), u);
		return this.getPermissionScheme().userCanGet(entity, u);
	}

	protected final ResponseEntity<?> userCanCreate(T entity, TownshendAuthentication u) {
		logger.info("checking user	POST {}	{}", entity.getClass().getSimpleName(), u);
		return this.getPermissionScheme().userCanCreate(entity, u);
	}

	protected final ResponseEntity<?> userCanUpdate(T entity, TownshendAuthentication u) {
		logger.info("checking user	PATCH {}	{}", entity.getClass().getSimpleName(), u);
		return this.getPermissionScheme().userCanUpdate(entity, u);
	}

	protected final ResponseEntity<?> userCanDelete(T entity, TownshendAuthentication u) {
		logger.info("checking user	DELETE {}	{}", entity.getClass().getSimpleName(), u);
		return this.getPermissionScheme().userCanDelete(entity, u);
	}

	protected final ResponseEntity<?> userCanList(List<T> list, TownshendAuthentication u) {
		logger.info("checking user	LIST {}	{}", list.isEmpty() ? "" : list.get(0).getClass().getSimpleName(), u);
		return this.getPermissionScheme().userCanList(list, u);
	}

	protected void prepareForCreate(T item) {
	}

	protected void prepareForUpdate(T entity, Map<String, Object> updates) {
	}

	@RequestMapping(method = RequestMethod.OPTIONS)
	public JsonSchema describe() {
		return EntitySchemaGenerator.getSchema(this.getResourceClass(), config.getEntities());
	}

	@RequestMapping(path = "{id}", method = RequestMethod.GET)
	public ResponseEntity<?> get(@PathVariable ID id, TownshendAuthentication u) {
		Optional<T> o = getRepo().findById(id);

		return handleGet(o, u);
	}

	protected ResponseEntity<?> handleGet(Optional<T> o, TownshendAuthentication u) {
		if (o.isPresent()) {
			T item = o.get();
			ResponseEntity<?> resp = userCanGet(item, u);
			if (resp == null) {
				return ResponseEntity.ok().body(item);
			} else {
				return resp;
			}
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody JsonNode json, TownshendAuthentication u) throws JsonProcessingException {
		T item = MAPPER.treeToValue(json, this.getResourceClass());
		@SuppressWarnings("unchecked")
		Map<String, Object> itemMap = MAPPER.treeToValue(json, Map.class);
		ResponseEntity<?> response = this.validateCreate(item, itemMap, u);
		if (response != null) {
			// TODO create an error object for consistent handling
			return response;
		} else {
			return this.handleCreate(item, u);
		}
	}

	protected ResponseEntity<?> handleCreate(T item, TownshendAuthentication u) {
		this.prepareForCreate(item);
		ResponseEntity<?> resp = userCanCreate(item, u);
		if (resp == null) {
			T savedItem = getRepo().save(item);
			savedItem = this.onCreateSuccess(savedItem, item);
			return ResponseEntity.ok().body(savedItem);
		} else {
			return resp;
		}
	}

	protected T onCreateSuccess(T savedItem, T originalItem) {
		return savedItem;
	}

	@RequestMapping(path = "{id}", method = RequestMethod.PATCH)
	public ResponseEntity<?> update(@RequestBody ObjectNode json, @PathVariable("id") ID id, TownshendAuthentication u)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, JsonProcessingException, NoSuchMethodException {

		Optional<T> o = getRepo().findById(id);
		if (o.isPresent()) {
			T entity = o.get();
			ResponseEntity<?> resp = userCanUpdate(entity, u);
			if (resp != null) {
				return resp;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> updates = MAPPER.treeToValue(json, Map.class);

			ResponseEntity<?> response = this.validateUpdate(entity, updates, u);
			if (response != null) {
				return response;
			}

			for (Iterator<String> iter = json.fieldNames(); iter.hasNext(); ) {
				String fieldName = iter.next();
				logger.info("PATCH {}\t{}", fieldName, json.get(fieldName));
			}
			//This MAPPER.updateValue version SHOULD:
			// * preserve field names (if json keys are not the same as field names)
			// * prevent issues with JsonIgnore'd fields (like password for example)
			entity = MAPPER.updateValue(entity, updates);
			this.prepareForUpdate(entity, updates);
			entity = getRepo().save(entity);
			return ResponseEntity.ok().body(entity);
		} else {
			return ResponseEntity.notFound().build();
		}
	}



	@RequestMapping(path = "{id}", method = RequestMethod.DELETE)
	public ResponseEntity<?> delete(@PathVariable ID id, TownshendAuthentication u) {

		Optional<T> o = getRepo().findById(id);

		if (o.isPresent()) {
			T entity = o.get();
			ResponseEntity<?> resp = this.userCanDelete(entity, u);
			if (resp == null) {
				getRepo().delete(entity);
				return ResponseEntity.noContent().build();
			} else {
				return resp;
			}
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<?> list(T probe,
			@RequestParam(required=false) String fieldFilter,
			@RequestParam Map<String,String> allRequestParams,
			TownshendAuthentication u) {
		List<T> list = getRepo().findAll(Example.of(probe));

		ResponseEntity<?> resp = this.userCanList(list, u);
		if (resp != null) {
			return resp;
		}

		if (fieldFilter != null) {
			List<String> fields = Arrays.asList(fieldFilter.split(FIELD_FILTER_SEPARATOR));
			logger.info("filtering to fields: {}", fields);
			JsonNode tree = MAPPER.valueToTree(list);
			List<Map<String, JsonNode>> filtered = new ArrayList<>();
			for (final JsonNode element : tree) {
				Map<String, JsonNode> filteredElement = new LinkedHashMap<>();
				for (String field : fields) {
					filteredElement.put(field, element.get(field));
				}
				filtered.add(filteredElement);
			}
			return ResponseEntity.ok().body(filtered);
		} else {
			return ResponseEntity.ok().body(list);
		}
	}

}
