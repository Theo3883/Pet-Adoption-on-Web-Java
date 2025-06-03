package com.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "MEDICALHISTORY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ANIMALID", referencedColumnName = "ANIMALID")
    private Animal animal;

    @Column(name = "VETNUMBER", length = 50)
    private String vetNumber;

    @Column(name = "RECORDDATE")
    private LocalDate recordDate;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "FIRST_AID_NOTED", length = 4000)
    private String firstAidNoted;
}