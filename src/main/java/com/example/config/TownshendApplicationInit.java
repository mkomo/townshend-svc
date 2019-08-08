package com.example.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.mkomo.townshend.bean.TownshendRoleUser;
import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.config.SecurityConfig;
import com.mkomo.townshend.repository.TownshendRoleRepository;
import com.mkomo.townshend.repository.TownshendUserRepository;

@Component
public class TownshendApplicationInit implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(TownshendApplicationInit.class);

	@Autowired
	private TownshendUserRepository userRepo;
	@Autowired
	private TownshendRoleRepository roleRepo;
	@Autowired
	private SecurityConfig securityConfig;

	@Override
	public void run(String... args) throws Exception {
		if (roleRepo.count() > 0) {
			logger.info("roles are already present in database.");
			return;
		}

		//TODO figure out how to init
		logger.info("adding role and user");
		TownshendRoleUser role = new TownshendRoleUser("ADMIN");
		role = roleRepo.save(role);
		TownshendUser user = new TownshendUser();
		user.setUsername("admin");
		user.setPassword(securityConfig.passwordEncoder().encode("admin"));
		user.setRoles(Arrays.asList(role));
		userRepo.save(user);
	}
}
