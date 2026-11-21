package com.smarttrans.ai.auth;

import com.smarttrans.ai.ServiceTestSupport;
import com.smarttrans.ai.api.ApiExceptions.Unauthorized;
import com.smarttrans.ai.auth.AuthResource.LoginRequest;
import com.smarttrans.ai.domain.Role;
import com.smarttrans.ai.domain.RoleCode;
import com.smarttrans.ai.domain.UserAccount;
import com.smarttrans.ai.security.JwtTokens;
import com.smarttrans.ai.security.Passwords;
import com.smarttrans.ai.user.UserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthResourceTest {

    private final UserService users = org.mockito.Mockito.mock(UserService.class);
    private final Passwords passwords = org.mockito.Mockito.mock(Passwords.class);
    private final JwtTokens tokens = org.mockito.Mockito.mock(JwtTokens.class);
    private AuthResource auth;

    @BeforeEach
    void setUp() {
        auth = new AuthResource();
        ServiceTestSupport.inject(auth, users, passwords, tokens);
    }

    @Test
    void unknownEmailIsRejectedBeforeThePasswordIsChecked() {
        when(users.findByEmail("admin@smarttrans.ai")).thenReturn(null);

        Unauthorized error = assertThrows(Unauthorized.class, () -> auth.login(login(" Admin@SmartTrans.AI ")));

        assertEquals("Invalid email or password", error.getMessage());
        verify(passwords, never()).matches(anyString(), any());
        verify(users, never()).recordLogin(any());
    }

    @Test
    void disabledAccountIsRejectedBeforeThePasswordIsChecked() {
        when(users.findByEmail("admin@smarttrans.ai")).thenReturn(user(false));

        Unauthorized error = assertThrows(Unauthorized.class, () -> auth.login(login("admin@smarttrans.ai")));

        assertEquals("User account is disabled", error.getMessage());
        verify(passwords, never()).matches(anyString(), any());
        verify(users, never()).recordLogin(any());
    }

    @Test
    void wrongPasswordDoesNotIssueAToken() {
        UserAccount user = user(true);
        when(users.findByEmail("admin@smarttrans.ai")).thenReturn(user);
        when(passwords.matches("Password1!", "hash")).thenReturn(false);

        Unauthorized error = assertThrows(Unauthorized.class, () -> auth.login(login("admin@smarttrans.ai")));

        assertEquals("Invalid email or password", error.getMessage());
        verify(users, never()).recordLogin(any());
        verify(tokens, never()).issue(any(), anyString(), any());
    }

    @Test
    void successfulLoginRecordsTheLoginAndReturnsABearerToken() {
        UUID userId = UUID.randomUUID();
        UserAccount user = user(true);
        ServiceTestSupport.setId(user, userId);
        when(users.findByEmail("admin@smarttrans.ai")).thenReturn(user);
        when(passwords.matches("Password1!", "hash")).thenReturn(true);
        when(tokens.issue(userId, "admin@smarttrans.ai", List.of("ADMIN"))).thenReturn("jwt-token");
        when(tokens.expirationMs()).thenReturn(86_400_000L);

        var response = auth.login(login(" Admin@SmartTrans.AI "));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86_400_000L, response.expiresInMs());
        assertEquals(userId, response.userId());
        assertEquals("admin@smarttrans.ai", response.email());
        assertEquals(Set.of(RoleCode.ADMIN), response.roleCodes());
        verify(users).recordLogin(userId);
    }

    private static LoginRequest login(String email) {
        return new LoginRequest(email, "Password1!");
    }

    private static UserAccount user(boolean enabled) {
        UserAccount user = new UserAccount();
        user.setEmail("admin@smarttrans.ai");
        user.setPasswordHash("hash");
        user.setFirstName("Admin");
        user.setLastName("SmartTrans");
        user.setEnabled(enabled);
        Role role = new Role();
        role.setCode(RoleCode.ADMIN);
        user.setRoles(Set.of(role));
        return user;
    }
}
