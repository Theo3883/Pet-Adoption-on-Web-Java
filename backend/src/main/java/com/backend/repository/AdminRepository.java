package com.backend.repository;

import com.backend.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    @Query("SELECT a FROM Admin a WHERE a.email = :email")
    Optional<Admin> findByEmail(@Param("email") String email);
    
    boolean existsByEmail(String email);
} 