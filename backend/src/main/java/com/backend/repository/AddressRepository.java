package com.backend.repository;

import com.backend.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    Optional<Address> findByUserUserId(Long userId);
    
    void deleteByUserUserId(Long userId);
} 