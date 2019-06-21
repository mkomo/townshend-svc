package com.mkomo.townshend.controller;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.helper.json.JsonSchema;
import com.mkomo.townshend.config.TownshendAccountConfig;
import com.mkomo.townshend.config.TownshendFieldConfig;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@RestController
@RequestMapping(TownshendSvcApplication.API_BASE_PATH + "/config")
public class TownshendConfigController implements ApplicationContextAware {

	@Autowired
	private TownshendAccountConfig accountConfig;

	private ApplicationContext applicationContext;

	//TODO move this to TownshendMvcConfigurer
	private static final MediaType MEDIA_TYPE_YAML = MediaType.valueOf("text/yaml");
	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@GetMapping
	public Object getApplicationConfig(@RequestParam(required=false) boolean yaml) throws JsonProcessingException {
		ApplicationConfig config =
				new ApplicationConfig(accountConfig, applicationContext, this.getEntityControllerBeanNames());
		if (yaml) {
			return ResponseEntity.ok()
					.contentType(MEDIA_TYPE_YAML)
					.body(YAML_MAPPER.writeValueAsString(config));
		}
		return config;
	}

	private List<String> getEntityControllerBeanNames() {
		return Arrays.asList(applicationContext.getBeanNamesForType(TownshendBaseController.class));
	}

	@GetMapping("/fields")
	public TownshendFieldConfig getFieldConfig() {
		return new TownshendFieldConfig();
	}

	public List<Class<?>> getEntities() {
		return getEntityControllerBeanNames().stream()
				.map(name -> ((TownshendBaseController<?,?>) applicationContext.getBean(name)).getResourceClass())
				.collect(Collectors.toList());
	}

	@Data
	@AllArgsConstructor
	public static class ApplicationConfig {

		private TownshendAccountConfig accountConfiguration;
		@Getter(AccessLevel.NONE)
		private ApplicationContext applicationContext;
		@Getter(AccessLevel.NONE)
		private List<String> entityControllerBeanNames;

		@SuppressWarnings("rawtypes")
		public List<String> getEntityNameList() {
			return entityControllerBeanNames.stream().map(name->
					getEntityName((TownshendBaseController)applicationContext.getBean(name)))

				.collect(Collectors.toList());
		}

		public Map<String, Object> getEntitySchemata() {
			Map<String, Object> schemata = new LinkedHashMap<>();
			for (TownshendBaseController<?,?> c : this.getControllers()) {
				schemata.put(getEntityName(c), c.describe());
			}
			return schemata;
		}

		public Map<String, Object> getEntityPermissions() {
			Map<String, Object> permissions = new LinkedHashMap<>();
			for (TownshendBaseController<?,?> c : this.getControllers()) {
				permissions.put(getEntityName(c), c.getPermissionScheme());
			}
			return permissions;
		}

		public Map<String, String> getEntityBasePaths() {
			Map<String, String> paths = new LinkedHashMap<>();
			for (TownshendBaseController<?,?> c : this.getControllers()) {
				RequestMapping a = c.getClass().getAnnotation(RequestMapping.class);
				String[] path = (a.path() == null || a.path().length == 0) ? a.value() : a.path();
				paths.put(getEntityName(c),
						(path == null || path.length == 0) ? null : path[0]);
			}
			return paths;
		}

		private List<? extends TownshendBaseController<?,?>> getControllers() {
			List<? extends TownshendBaseController<?,?>> controllers = entityControllerBeanNames.stream()
					.map(name->((TownshendBaseController<?,?>)applicationContext.getBean(name)))
					.collect(Collectors.toList());
			return controllers;
		}

		private static String getEntityName(TownshendBaseController<?,?> controller) {
			return JsonSchema.schemaNameFromClass(controller.getResourceClass());
		}
	}
}
