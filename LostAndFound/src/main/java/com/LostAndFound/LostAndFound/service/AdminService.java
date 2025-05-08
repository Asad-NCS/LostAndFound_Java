package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.AdminDTO;
import com.LostAndFound.LostAndFound.model.Admin;
import com.LostAndFound.LostAndFound.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepository;

    public AdminDTO createAdmin(AdminDTO adminDTO) {
        Admin admin = new Admin();
        admin.setUsername(adminDTO.getUsername());
        admin.setEmail(adminDTO.getEmail());
        admin.setPassword(adminDTO.getPassword());

        Admin savedAdmin = adminRepository.save(admin);
        return toDTO(savedAdmin);
    }

    public AdminDTO getAdminById(Long id) {
        return adminRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    public AdminDTO updateAdmin(Long id, AdminDTO adminDTO) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        admin.setUsername(adminDTO.getUsername());
        admin.setEmail(adminDTO.getEmail());
        if (adminDTO.getPassword() != null) {
            admin.setPassword(adminDTO.getPassword());
        }

        return toDTO(adminRepository.save(admin));
    }

    public void deleteAdmin(Long id) {
        adminRepository.deleteById(id);
    }

    private AdminDTO toDTO(Admin admin) {
        AdminDTO dto = new AdminDTO();
        dto.setId(admin.getId());
        dto.setUsername(admin.getUsername());
        dto.setEmail(admin.getEmail());
        // Never return password in DTO
        return dto;
    }
}
