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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
@EnableConfigurationProperties(AdminBootstrapProperties.class)
@RequiredArgsConstructor
@Slf4j
public class ProdAdminBootstrap implements ApplicationRunner {

	private final AdminBootstrapProperties properties;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(properties.adminEmail()) || !StringUtils.hasText(properties.adminPassword())) {
			return;
		}
		if (userRepository.count() > 0) {
			return;
		}

		Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
				.orElseThrow(() -> new IllegalStateException("ADMIN role missing from database seed"));

		String firstName = StringUtils.hasText(properties.adminFirstName()) ? properties.adminFirstName() : "Admin";
		String lastName = StringUtils.hasText(properties.adminLastName()) ? properties.adminLastName() : "SmartTrans";

		User admin = User.builder()
				.email(properties.adminEmail().trim().toLowerCase())
				.passwordHash(passwordEncoder.encode(properties.adminPassword()))
				.firstName(firstName)
				.lastName(lastName)
				.enabled(true)
				.roles(Set.of(adminRole))
				.build();

		userRepository.save(admin);
		log.info("Prod admin created: {}", admin.getEmail());
	}
}
