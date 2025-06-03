package com.backend.repository;

import com.backend.model.FeedingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedingScheduleRepository extends JpaRepository<FeedingSchedule, Long> {

    Optional<FeedingSchedule> findByAnimalAnimalId(Long animalId);

    void deleteByAnimalAnimalId(Long animalId);

    @Query(value = """
            SELECT
                fs.ID,
                fs.ANIMALID,
                LISTAGG(ft.COLUMN_VALUE, ',') AS FEEDING_TIME_STRING,
                fs.FOOD_TYPE,
                fs.NOTES
            FROM FEEDINGSCHEDULE fs,
                 TABLE(fs.FEEDING_TIME) ft
            WHERE fs.ANIMALID = :animalId
            GROUP BY fs.ID, fs.ANIMALID, fs.FOOD_TYPE, fs.NOTES
            """, nativeQuery = true)
    List<Object[]> findFeedingScheduleWithExtractedTimes(@Param("animalId") Long animalId);

    @Modifying
    @Query(value = """
            INSERT INTO FEEDINGSCHEDULE (ANIMALID, FEEDING_TIME, FOOD_TYPE, NOTES)
            VALUES (:animalId,
                    feeding_time_array(:feedingTimesStr),
                    :foodType,
                    :notes)
            """, nativeQuery = true)
    void insertFeedingScheduleWithVArray(@Param("animalId") Long animalId,
            @Param("feedingTimesStr") String feedingTimesStr,
            @Param("foodType") String foodType,
            @Param("notes") String notes);
}