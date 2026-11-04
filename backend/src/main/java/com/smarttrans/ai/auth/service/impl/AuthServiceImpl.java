package com.smarttrans.ai.auth.service.impl;

import com.smarttrans.ai.activity.ActivityEntityType;
import com.smarttrans.ai.activity.enums.ActivityAction;
import com.smarttrans.ai.activity.service.ActivityService;
import com.smarttrans.ai.auth.dto.LoginRequest;
import com.smarttrans.ai.auth.dto.LoginResponse;
import com.smarttrans.ai.auth.service.AuthService;
import com.smarttrans.ai.common.enums.RoleCode;
import com.smarttrans.ai.role.entity.Role;
import com.smarttrans.ai.security.jwt.JwtService;
import com.smarttrans.ai.security.service.UserPrincipal;
import com.smarttrans.ai.user.entity.User;
import com.smarttrans.ai.user.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final ActivityService activityService;

	@Override
	public LoginResponse login(LoginRequest request) {
		String email = request.getEmail().trim().toLowerCase();

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.getPassword()));

		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		String accessToken = jwtService.generateToken(principal);

		User user = userRepository.findByEmailWithRoles(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated user missing from database"));
		user.setLastLoginAt(Instant.now());
		activityService.record(
				user.getId(),
				ActivityAction.LOGIN,
				ActivityEntityType.USER,
				user.getId(),
				ActivityEntityType.metadata("email", user.getEmail()));

		Set<RoleCode> roleCodes = user.getRoles().stream()
				.map(Role::getCode)
				.collect(Collectors.toSet());

		return LoginResponse.builder()
				.accessToken(accessToken)
				.tokenType("Bearer")
				.expiresInMs(jwtService.getExpirationMs())
				.userId(user.getId())
				.email(user.getEmail())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.roleCodes(roleCodes)
				.build();
	}
}
