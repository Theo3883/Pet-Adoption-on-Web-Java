package com.backend.repository;

import com.backend.model.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    
    List<Animal> findByUserUserId(Long userId);
    
    List<Animal> findBySpecies(String species);
    
    @Modifying
    @Query("UPDATE Animal a SET a.views = a.views + 1 WHERE a.animalId = :animalId")
    void incrementViews(@Param("animalId") Long animalId);
    
    @Query("SELECT a FROM Animal a " +
           "JOIN a.user u " +
           "JOIN u.address addr " +
           "WHERE addr.city = :city " +
           "ORDER BY a.views DESC, a.createdAt DESC")
    List<Animal> findTopAnimalsByCity(@Param("city") String city);
    
    @Query("SELECT a.breed, COUNT(a) as breedCount " +
           "FROM Animal a " +
           "WHERE a.species = :species " +
           "GROUP BY a.breed " +
           "ORDER BY breedCount DESC")
    List<Object[]> findPopularBreedsBySpecies(@Param("species") String species);
} 