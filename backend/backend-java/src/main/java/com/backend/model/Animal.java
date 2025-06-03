package com.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ANIMAL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANIMALID")
    private Long animalId;

    @NotBlank(message = "Animal name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(name = "NAME", length = 100)
    private String name;

    @Size(max = 100, message = "Breed must not exceed 100 characters")
    @Column(name = "BREED", length = 100)
    private String breed;

    @Size(max = 100, message = "Species must not exceed 100 characters")
    @Column(name = "SPECIES", length = 100)
    private String species;

    @Column(name = "AGE")
    private Integer age;

    @Column(name = "VIEWS")
    private Integer views = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "GENDER", length = 10)
    private Gender gender;

    @CreationTimestamp
    @Column(name = "CREATEDAT")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USERID", referencedColumnName = "USERID")
    @NotNull
    private User user;

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicalHistory> medicalHistories;

    @OneToOne(mappedBy = "animal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private FeedingSchedule feedingSchedule;

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MultiMedia> multimedia;

    @OneToOne(mappedBy = "animal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Relations relations;

    public enum Gender {
        male, female
    }
}