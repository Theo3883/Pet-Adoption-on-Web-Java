package com.backend.service;

import com.backend.model.Admin;
import com.backend.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    
    private final AdminRepository adminRepository;
    private final JwtService jwtService;
    
    public String authenticateAdmin(String email, String password) {
        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        
        if (adminOpt.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }
        
        Admin admin = adminOpt.get();
        if (!password.equals(admin.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        
        return jwtService.generateToken(admin.getAdminId(), admin.getEmail(), true);
    }
    
    public Optional<Admin> findById(Long adminId) {
        return adminRepository.findById(adminId);
    }
} 