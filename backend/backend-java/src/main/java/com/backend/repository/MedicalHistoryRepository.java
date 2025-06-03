package com.backend.repository;

import com.backend.model.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {

    List<MedicalHistory> findByAnimalAnimalId(Long animalId);

    void deleteByAnimalAnimalId(Long animalId);
}