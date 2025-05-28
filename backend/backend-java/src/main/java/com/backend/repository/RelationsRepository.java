package com.backend.repository;

import com.backend.model.Relations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RelationsRepository extends JpaRepository<Relations, Long> {
    
    Optional<Relations> findByAnimalAnimalId(Long animalId);
    
    void deleteByAnimalAnimalId(Long animalId);
} 