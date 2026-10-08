package com.booking.admin;

import java.time.Instant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.booking.config.OpenApiConfig;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin: authentication")
public class AdminAuthController {

    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    public record LinkRequest(@NotBlank @Email @Size(max = 320) String email) {
    }

    public record VerifyRequest(@NotBlank @Size(max = 200) String token) {
    }

    public record SessionResponse(String token, Instant expiresAt, String email) {
    }

    public record MeResponse(Long id, String email) {
    }

    @PostMapping("/request-link")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request a magic login link",
            description = "Always returns 202, whether or not the email belongs to an administrator.")
    public void requestLink(@Valid @RequestBody LinkRequest request) {
        authService.requestLink(request.email());
    }

    @PostMapping("/verify")
    @Operation(summary = "Redeem a magic-link token for a session token",
            description = "Tokens are single-use and short-lived. Send the returned token as 'Authorization: Bearer <token>'.")
    public SessionResponse verify(@Valid @RequestBody VerifyRequest request) {
        AdminAuthService.IssuedSession session = authService.verify(request.token());
        return new SessionResponse(session.token(), session.expiresAt(), session.email());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = OpenApiConfig.ADMIN_SECURITY_SCHEME)
    @Operation(summary = "The currently authenticated administrator")
    public MeResponse me(@AuthenticationPrincipal AdminPrincipal principal) {
        return new MeResponse(principal.id(), principal.email());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = OpenApiConfig.ADMIN_SECURITY_SCHEME)
    @Operation(summary = "Revoke the current session token")
    public void logout(HttpServletRequest request) {
        String token = BearerTokenAuthenticationFilter.extractToken(request);
        if (token != null) {
            authService.logout(token);
        }
    }
}
