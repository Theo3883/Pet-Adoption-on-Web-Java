package com.backend.repository;

import com.backend.model.MultiMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultiMediaRepository extends JpaRepository<MultiMedia, Long> {

    List<MultiMedia> findByAnimalAnimalId(Long animalId);

    @Query("SELECT m FROM MultiMedia m WHERE m.animal.animalId = :animalId AND m.media = 'photo' ORDER BY m.uploadDate DESC")
    List<MultiMedia> findPhotosByAnimalId(@Param("animalId") Long animalId);

    @Query("SELECT m FROM MultiMedia m WHERE m.animal.animalId = :animalId AND m.media = 'photo' ORDER BY m.uploadDate DESC LIMIT 1")
    MultiMedia findFirstPhotoByAnimalId(@Param("animalId") Long animalId);

    void deleteByAnimalAnimalId(Long animalId);
}