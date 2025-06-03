package com.backend.repository;

import com.backend.model.Newsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {

    List<Newsletter> findByUserUserId(Long userId);

    @Query("SELECT n FROM Newsletter n " +
            "JOIN n.user u " +
            "WHERE n.species = :species AND n.isActive = true")
    List<Newsletter> findActiveSubscribersBySpecies(@Param("species") String species);

    void deleteByUserUserIdAndSpecies(Long userId, String species);

    boolean existsByUserUserIdAndSpecies(Long userId, String species);
}