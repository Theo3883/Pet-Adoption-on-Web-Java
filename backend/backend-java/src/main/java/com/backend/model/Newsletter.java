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

@Entity
@Table(name = "NEWSLETTER", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"USERID", "SPECIES"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Newsletter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @NotBlank(message = "Species is required")
    @Size(max = 100, message = "Species must not exceed 100 characters")
    @Column(name = "SPECIES", length = 100)
    private String species;
    
    @Column(name = "ISACTIVE")
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "SUBSCRIBEDAT")
    private LocalDateTime subscribedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USERID", referencedColumnName = "USERID")
    @NotNull
    private User user;
} 