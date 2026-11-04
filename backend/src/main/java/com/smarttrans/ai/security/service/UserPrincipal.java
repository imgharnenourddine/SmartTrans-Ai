package com.smarttrans.ai.security.service;

import com.smarttrans.ai.user.entity.User;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UserPrincipal implements UserDetails {

	private final UUID id;
	private final String email;
	private final String password;
	private final boolean enabled;
	private final Collection<? extends GrantedAuthority> authorities;

	public UserPrincipal(User user) {
		this.id = user.getId();
		this.email = user.getEmail();
		this.password = user.getPasswordHash();
		this.enabled = user.isEnabled();
		this.authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode().name()))
				.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}
}
