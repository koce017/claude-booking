package com.booking.company;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.booking.company.CompanyDtos.AdminCompanyResponse;
import com.booking.company.CompanyDtos.CreateCompanyRequest;
import com.booking.config.OpenApiConfig;

@RestController
@RequestMapping("/api/admin/companies")
@Tag(name = "Admin: companies")
@SecurityRequirement(name = OpenApiConfig.ADMIN_SECURITY_SCHEME)
public class AdminCompanyController {

    private final CompanyService companyService;

    public AdminCompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a company", description = "Only the name is required; all settings use defaults.")
    public AdminCompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) {
        return AdminCompanyResponse.from(companyService.create(request.name()));
    }

    @GetMapping
    @Operation(summary = "List companies", description = "Active companies only unless includeInactive=true.")
    public List<AdminCompanyResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        List<Company> companies = includeInactive ? companyService.listAll() : companyService.listActive();
        return companies.stream().map(AdminCompanyResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a company (active or removed)")
    public AdminCompanyResponse get(@PathVariable Long id) {
        return AdminCompanyResponse.from(companyService.get(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove (deactivate) a company",
            description = "Soft delete: hidden from the public, history kept, pending requests are denied.")
    public void remove(@PathVariable Long id) {
        companyService.deactivate(id);
    }
}
