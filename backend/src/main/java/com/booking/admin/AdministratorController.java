package com.booking.admin;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.booking.config.OpenApiConfig;

@RestController
@RequestMapping("/api/admin/administrators")
@Tag(name = "Admin: administrators")
@SecurityRequirement(name = OpenApiConfig.ADMIN_SECURITY_SCHEME)
public class AdministratorController {

    private final AdministratorService administratorService;

    public AdministratorController(AdministratorService administratorService) {
        this.administratorService = administratorService;
    }

    public record AddRequest(@NotBlank @Email @Size(max = 320) String email) {
    }

    public record AdministratorResponse(Long id, String email, Instant createdAt) {

        static AdministratorResponse from(Administrator a) {
            return new AdministratorResponse(a.getId(), a.getEmail(), a.getCreatedAt());
        }
    }

    @GetMapping
    @Operation(summary = "List active administrators")
    public List<AdministratorResponse> list() {
        return administratorService.listActive().stream().map(AdministratorResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an administrator", description = "They can then sign in with a magic link.")
    public AdministratorResponse add(@Valid @RequestBody AddRequest request) {
        return AdministratorResponse.from(administratorService.add(request.email()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove an administrator", description = "Their sessions end immediately. Not allowed for yourself.")
    public void remove(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal principal) {
        administratorService.deactivate(id, principal.id());
    }
}
