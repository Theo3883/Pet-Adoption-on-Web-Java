package com.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ADMINS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADMINDATA_ADMINID")
    private Long adminId;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Column(name = "ADMINDATA_EMAIL", unique = true, length = 255)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @Column(name = "ADMINDATA_PASSWORD", length = 100)
    private String password;
    
    @CreationTimestamp
    @Column(name = "ADMINDATA_CREATEDAT")
    private LocalDateTime createdAt;
} 