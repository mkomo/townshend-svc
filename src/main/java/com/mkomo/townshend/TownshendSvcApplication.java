package com.mkomo.townshend;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import com.example.config.TownshendSecurityConfig;

@SpringBootApplication
@ComponentScan(basePackageClasses = {
	TownshendSvcApplication.class,
	Jsr310JpaConverters.class,
	TownshendSecurityConfig.class
})
public class TownshendSvcApplication extends SpringBootServletInitializer {

	//TODO get this from config somehow?
	public static final String API_BASE_PATH = "/api";

	private static final Logger logger = LoggerFactory.getLogger("COMPONENTLIST");

	@PostConstruct
	void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext context =
				SpringApplication.run(TownshendSvcApplication.class, args);
		for (String name : context.getBeanDefinitionNames()) {
			logger.debug("loaded bean: " + name);
		}
	}
}
