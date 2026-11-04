package com.smarttrans.ai.security.jwt;

import com.smarttrans.ai.security.service.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey secretKey;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(UserPrincipal principal) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + properties.expirationMs());

		List<String> roles = principal.getAuthorities().stream()
				.map(authority -> authority.getAuthority().replace("ROLE_", ""))
				.toList();

		return Jwts.builder()
				.subject(principal.getId().toString())
				.claim("email", principal.getUsername())
				.claim("roles", roles)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(secretKey)
				.compact();
	}

	public boolean isValid(String token) {
		try {
			parseClaims(token);
			return true;
		}
		catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	public UUID extractUserId(String token) {
		return UUID.fromString(parseClaims(token).getSubject());
	}

	public String extractEmail(String token) {
		return parseClaims(token).get("email", String.class);
	}

	public long getExpirationMs() {
		return properties.expirationMs();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
