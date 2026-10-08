package com.booking.company;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking.company.CompanyDtos.PublicCompanyResponse;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Public: companies")
public class PublicCompanyController {

    private final CompanyService companyService;

    public PublicCompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "List active companies")
    public List<PublicCompanyResponse> list() {
        return companyService.listActive().stream().map(PublicCompanyResponse::from).toList();
    }

    @GetMapping("/{companyId}")
    @Operation(summary = "Get an active company", description = "Returns 404 for unknown or removed companies.")
    public PublicCompanyResponse get(@PathVariable Long companyId) {
        return PublicCompanyResponse.from(companyService.getActive(companyId));
    }
}
