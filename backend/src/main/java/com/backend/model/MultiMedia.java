package com.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "MULTIMEDIA")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "MEDIA", length = 10)
    private MediaType media;
    
    @Size(max = 1000, message = "URL must not exceed 1000 characters")
    @Column(name = "URL", length = 1000)
    private String url;
    
    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    @Column(name = "DESCRIPTION", length = 4000)
    private String description;
    
    @Column(name = "UPLOAD_DATE")
    private LocalDate uploadDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANIMALID", referencedColumnName = "ANIMALID")
    @NotNull
    private Animal animal;
    
    public enum MediaType {
        photo, video, audio
    }
} 