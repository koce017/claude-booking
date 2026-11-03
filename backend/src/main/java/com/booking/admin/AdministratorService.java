package com.booking.admin;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.common.error.ConflictException;
import com.booking.common.error.NotFoundException;

/**
 * Adding and removing administrators. Removal only deactivates the account, which
 * ends its sessions immediately; adding the same email again reactivates it.
 */
@Service
public class AdministratorService {

    private final AdministratorRepository administratorRepository;

    public AdministratorService(AdministratorRepository administratorRepository) {
        this.administratorRepository = administratorRepository;
    }

    @Transactional(readOnly = true)
    public List<Administrator> listActive() {
        return administratorRepository.findByActiveTrueOrderByIdAsc();
    }

    @Transactional
    public Administrator add(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return administratorRepository.findByEmailIgnoreCase(normalized)
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new ConflictException("ADMIN_EXISTS", "This email already belongs to an administrator");
                    }
                    existing.setActive(true);
                    return existing;
                })
                .orElseGet(() -> administratorRepository.save(new Administrator(normalized)));
    }

    @Transactional
    public void deactivate(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new ConflictException("CANNOT_REMOVE_SELF", "You cannot remove your own administrator account");
        }
        Administrator admin = administratorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Administrator", id));
        admin.setActive(false);
    }
}
