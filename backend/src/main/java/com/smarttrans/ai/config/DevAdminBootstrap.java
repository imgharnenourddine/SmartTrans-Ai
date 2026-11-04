package com.smarttrans.ai.config;

import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.role.entity.Role;
import com.smarttrans.ai.role.repository.RoleRepository;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a local admin account when the database has no users (dev / it profiles).
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAdminBootstrap implements ApplicationRunner {

	public static final String DEFAULT_ADMIN_EMAIL = "admin@smarttrans.ai";
	public static final String DEFAULT_ADMIN_PASSWORD = "Password1!";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			return;
		}

		Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
				.orElseThrow(() -> new IllegalStateException("ADMIN role missing from database seed"));

		User admin = User.builder()
				.email(DEFAULT_ADMIN_EMAIL)
				.passwordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
				.firstName("Admin")
				.lastName("SmartTrans")
				.enabled(true)
				.roles(Set.of(adminRole))
				.build();

		userRepository.save(admin);
		log.info("Dev admin created: {} / {}", DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
	}
}
