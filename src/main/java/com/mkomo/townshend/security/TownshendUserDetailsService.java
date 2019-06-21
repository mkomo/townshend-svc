package com.mkomo.townshend.security;

import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mkomo.townshend.bean.TownshendUser;
import com.mkomo.townshend.repository.TownshendUserRepository;

@Service
public class TownshendUserDetailsService implements UserDetailsService {

	private static final Predicate<String> IS_EMAIL = t -> t.contains("@");

	@Autowired
	private TownshendUserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {

		Optional<TownshendUser> o;
		if (IS_EMAIL.test(usernameOrEmail)) {
			o = userRepository.findByEmail(usernameOrEmail);
		} else {
			o = userRepository.findByUsername(usernameOrEmail);
		}
		if (o.isPresent()) {
			return new TownshendUserDetails(o.get());
		} else {
			throw new UsernameNotFoundException("No user with given name found");
		}
	}

}
