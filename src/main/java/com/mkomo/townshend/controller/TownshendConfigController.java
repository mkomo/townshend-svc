package com.mkomo.townshend.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mkomo.townshend.TownshendSvcApplication;
import com.mkomo.townshend.bean.helper.json.JsonSchema;
import com.mkomo.townshend.config.TownshendAccountConfig;
import com.mkomo.townshend.config.TownshendFieldConfig;
import com.mkomo.townshend.config.TownshendFrontendApplicationConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javax.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
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

	@RequestMapping("/apispec/classhierarchy")
	@ResponseBody
	public List<FClassHierarchy> getClassHierarchy() throws IOException, ClassNotFoundException {

		JarFile jar = null;
		try {
			String jarName = System.getenv("HOME") + "/workspaces/ZapposMyAccount/build/ZapposMyAccount/ZapposMyAccount-1.0/RHEL5_64/DEV.STD.PTHREAD/build//lib/ZapposMyAccount-1.0.jar";
			jar = new JarFile(jarName);
			List<FClassHierarchy> ch = new ArrayList<>();
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (!entry.getName().endsWith(".class")) {
					continue;
				}

				ClassParser parser =
					new ClassParser(jarName, entry.getName());
				JavaClass javaClass = parser.parse();

				ch.add(FClassHierarchy.of(javaClass));
			}
			return ch;
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
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

	@RequestMapping("/apispec/usage")
	@ResponseBody
	public Object getApiUsage(@RequestBody List<MappingWithUsage> paths) {
		List<RequestMappingSummary> apiSpec = getApiSpec();
		Map<RequestMappingSummary, Integer> usage = apiSpec.stream()
			.collect(Collectors.toMap(Function.identity(), r->0));

		for (MappingWithUsage req : paths) {
			RequestMappingSummary rms = getEndpoint(req, apiSpec);
			usage.put(rms, usage.getOrDefault(rms, 0) + req.getCount());
		}

		return usage.entrySet().stream()
			.map(e->{e.getKey().setCount(e.getValue()); return e.getKey();})
			.sorted(Comparator.comparing(RequestMappingSummary::getCount, Comparator.nullsFirst(Comparator.naturalOrder())).reversed()
				.thenComparing(r->(r.getClassName() == null ? "" : r.getClassName().getName()))
				.thenComparing(Comparator.comparing(RequestMappingSummary::getName, Comparator.nullsFirst(Comparator.naturalOrder())))
			)
			.collect(Collectors.toList());
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
			return method + " " + getPath();
		}

	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class MappingWithUsage extends Mapping {
		private Integer count;

		@Override
		public String toString() {
			return super.toString() + ": " + count;
		}
	}

	@Data
	public static class RequestMappingSummary {

		private Set<String> methods;
		private Set<String> patterns;
		private Class<?> className;
		private String name;
		private Integer lineNumber;
		private Boolean hasResponseBody;
		private String returnType;
		private Integer count;
		@JsonIgnore
		private PatternsRequestCondition patternCondition;
		@JsonIgnore
		private RequestMappingInfo info;

		public RequestMappingSummary() {
		}

		public RequestMappingSummary(String method, String requestURI) {
			this.methods = Collections.singleton(method);
			this.patterns = Collections.singleton(requestURI);
		}

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

			try {
				ClassPool pool = ClassPool.getDefault();
				CtClass cc = pool.get(summary.getClassName().getName());
				CtMethod m = cc.getDeclaredMethod(summary.getName());
				summary.lineNumber = m.getMethodInfo().getLineNumber(0);
			} catch (Exception e) {
				LOG.warn("Failed to set lineNumber", e);
			}
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

	@Data
	private static class FClassHierarchy {

		private final String className;
		private final List<String> classHierarchy;
		private final List<String> interfaces;

		public FClassHierarchy(String className, List<String> classHierarchy, List<String> interfaces) {
			this.className = className;
			this.classHierarchy = classHierarchy;
			this.interfaces = interfaces;
		}

		static FClassHierarchy of(JavaClass javaClass) throws ClassNotFoundException {

			List<String> classHierarchy = Arrays.stream(javaClass.getSuperClasses())
				.map(jc -> jc.getClassName())
				.filter(jc -> !jc.equals(Object.class.getName()))
				.collect(Collectors.toList());
			List<String> interfaces = new ArrayList();


			List<JavaClass> hierarchy = Stream
				.concat(Stream.of(javaClass), Arrays.stream(javaClass.getSuperClasses()))
				.collect(Collectors.toList());
			hierarchy.stream()
				.filter(jc -> !jc.getClassName().equals(Object.class.getName()))
				.forEach(jc -> {
					interfaces.addAll(Arrays.asList(jc.getInterfaceNames()));
				});
			return new FClassHierarchy(javaClass.getClassName(), classHierarchy, interfaces);
		}
	}
}
