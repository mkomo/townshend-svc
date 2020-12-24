package com.mkomo.townshend.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.helper.json.JsonSchema;
import com.mkomo.townshend.config.TownshendAccountConfig;
import com.mkomo.townshend.config.TownshendFieldConfig;
import com.mkomo.townshend.config.TownshendFrontendApplicationConfig;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RestController
@RequestMapping(TownshendSvcApplication.API_BASE_PATH + "/config")
public class TownshendConfigController implements ApplicationContextAware {

	@Autowired
	RequestMappingHandlerMapping handlerMapping;

	@Autowired
	private TownshendAccountConfig accountConfig;

	@Autowired
	private TownshendFrontendApplicationConfig frontendApplicationConfig;

	private ApplicationContext applicationContext;

	//TODO move this to TownshendMvcConfigurer
	private static final MediaType MEDIA_TYPE_YAML = MediaType.valueOf("text/yaml");
	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
	private static final Logger LOG = LoggerFactory.getLogger(TownshendConfigController.class);

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@GetMapping
	public Object getApplicationConfig(@RequestParam(required=false) boolean yaml) throws JsonProcessingException {
		ApplicationConfig config =
				new ApplicationConfig(frontendApplicationConfig.getFrontendConfig(), accountConfig, applicationContext, this.getEntityControllerBeanNames());
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

		private Object frontendApplicationConfig;
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

	@RequestMapping("/apispec")
	@ResponseBody
	public List<RequestMappingSummary> getApiSpec() {
		Map<RequestMappingInfo, HandlerMethod> methods = this.handlerMapping
			.getHandlerMethods();

		List<RequestMappingSummary> endpoints = new ArrayList<>();
		for (RequestMappingInfo method : methods.keySet()) {
			endpoints.add(RequestMappingSummary.of(method, methods.get(method)));
		}
		return endpoints;
	}

	@RequestMapping("/apispec/mappings")
	@ResponseBody
	public Object getApiMappings(@RequestBody List<Mapping> paths) {
		List<RequestMappingSummary> apiSpec = getApiSpec();
		Map<String, RequestMappingSummary> methodByRequest = new LinkedHashMap<>();
		for (Mapping req : paths) {
			methodByRequest.put(req.toString(), getEndpoint(req, apiSpec));
		}
		return methodByRequest;
	}

	private RequestMappingSummary getEndpoint(Mapping req, List<RequestMappingSummary> apiSpec) {
		LOG.debug("Finding endpoint for mapping: {} call to {}", req.getMethod(), req.getPath());
		MockHttpServletRequest request = new MockHttpServletRequest(req.getMethod(), req.getPath());
		return getEndpoint(request, apiSpec);
	}

	private RequestMappingSummary getEndpoint(HttpServletRequest request, List<RequestMappingSummary> apiSpec) {
		LOG.debug("Finding endpoint for httpservletrequest: {} call to {}", request.getMethod(), request.getRequestURI());
		List<RequestMappingSummary> endpoints = new ArrayList<>();
		for (RequestMappingSummary endpoint : apiSpec) {
			if (endpoint.matches(request)) {
				endpoints.add(endpoint);
			}
		}
		if (endpoints.isEmpty()) return null;
		endpoints.sort(new Comparator<RequestMappingSummary>() {
			public int compare(RequestMappingSummary info1, RequestMappingSummary info2) {
				return info1.info.compareTo(info2.info, request);
			}
		});
		return endpoints.get(0);
	}

	@Data
	public static class Mapping {
		private String method;
		private String path;

		@Override
		public String toString() {
			return method + " " + path;
		}
	}

	public static class RequestMappingSummary {

		public Set<String> methods;
		public Set<String> patterns;
		public Class<?> className;
		public String name;
		public Boolean hasResponseBody;
		public String returnType;
		@JsonIgnore
		private PatternsRequestCondition patternCondition;
		@JsonIgnore
		private RequestMappingInfo info;

		public static RequestMappingSummary of(RequestMappingInfo method, HandlerMethod handlerMethod) {
			RequestMappingSummary summary = new RequestMappingSummary();
			summary.info = method;
			summary.patterns = method.getPatternsCondition().getPatterns();
			summary.patternCondition = method.getPatternsCondition();
			summary.methods = method.getMethodsCondition().getMethods().stream().map(m->m.toString()).collect(
				Collectors.toSet());
			summary.className = handlerMethod.getMethod().getDeclaringClass();
			summary.name = handlerMethod.getMethod().getName();
			summary.returnType = handlerMethod.getMethod().getGenericReturnType().getTypeName();
			summary.hasResponseBody = handlerMethod.getMethod().getAnnotation(ResponseBody.class) != null ||
				handlerMethod.getMethod().getDeclaringClass().getAnnotation(ResponseBody.class) != null ||
				handlerMethod.getMethod().getDeclaringClass().getAnnotation(RestController.class) != null;
			return summary;
		}

		public boolean matches(Mapping req) {
			return (methods.contains(req.method) || methods.isEmpty()) && patternCondition.getMatchingPatterns(req.path).size() > 0;
		}

		public boolean matches(HttpServletRequest req) {
			LOG.debug("testing path {} against conditions {}", req.getRequestURI(), patternCondition.getPatterns());
			return (methods.contains(req.getMethod()) || methods.isEmpty()) && patternCondition.getMatchingPatterns(req.getRequestURI()).size() > 0;
		}
	}
}
