package com.smarttrans.ai.security.service;

import com.smarttrans.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		String normalized = email.trim().toLowerCase();
		return userRepository.findByEmailWithRoles(normalized)
				.map(UserPrincipal::new)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));
	}
}
