package com.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "ADDRESS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADDRESSID")
    private Long addressId;

    @Size(max = 255, message = "Street must not exceed 255 characters")
    @Column(name = "STREET", length = 255)
    private String street;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Column(name = "CITY", length = 100)
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Column(name = "STATE", length = 100)
    private String state;

    @Column(name = "ZIPCODE")
    private Integer zipCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Column(name = "COUNTRY", length = 100)
    private String country;

    @OneToOne
    @JoinColumn(name = "USERID", referencedColumnName = "USERID", unique = true)
    @NotNull
    private User user;
}