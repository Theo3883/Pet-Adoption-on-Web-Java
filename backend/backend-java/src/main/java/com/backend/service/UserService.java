package com.backend.service;

import com.backend.dto.UserLoginRequest;
import com.backend.dto.UserResponse;
import com.backend.dto.UserSignupRequest;
import com.backend.dto.AddressResponse;
import com.backend.exception.AuthenticationException;
import com.backend.exception.ResourceNotFoundException;
import com.backend.exception.ValidationException;
import com.backend.model.User;
import com.backend.model.Address;
import com.backend.repository.UserRepository;
import com.backend.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final JwtService jwtService;
      public UserResponse createUser(UserSignupRequest request) {
        if (request.getFirstName() == null || request.getLastName() == null || 
            request.getEmail() == null || request.getPassword() == null || 
            request.getPhone() == null || request.getAddress() == null) {
            throw ValidationException.missingRequiredField("required user fields");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ValidationException.emailAlreadyExists();
        }
        
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); 
        user.setPhone(request.getPhone());
        
        User savedUser = userRepository.save(user);
        
        if (request.getAddress() != null) {
            Address address = new Address();
            address.setUser(savedUser);
            address.setStreet(request.getAddress().getStreet());
            address.setCity(request.getAddress().getCity());
            address.setState(request.getAddress().getState());
            
            try {
                if (request.getAddress().getZipCode() != null && !request.getAddress().getZipCode().trim().isEmpty()) {
                    address.setZipCode(Integer.valueOf(request.getAddress().getZipCode()));
                }
            } catch (NumberFormatException e) {
                address.setZipCode(null);
            }
            
            address.setCountry(request.getAddress().getCountry());
            
            addressRepository.save(address);
        }
        
        return convertToUserResponse(savedUser);
    }
    
    public String authenticateUser(UserLoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        
        if (userOpt.isEmpty()) {
            throw AuthenticationException.invalidCredentials();
        }
        
        User user = userOpt.get();
        if (!request.getPassword().equals(user.getPassword())) {
            throw AuthenticationException.invalidCredentials();
        }
        
        String createdAtString = "";
        try {
            if (user.getCreatedAt() != null) {
                createdAtString = user.getCreatedAt().toString();
            }
        } catch (Exception e) {
            createdAtString = "";
        }
        
        return jwtService.generateUserToken(
            user.getUserId(), 
            user.getEmail(), 
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            createdAtString
        );
    }
    
    public List<UserResponse> getAllUsersWithDetails() {
        return userRepository.findAllWithDetails()
                .stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.userNotFound(userId);
        }
        userRepository.deleteById(userId);
    }
    
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
    
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setCreatedAt(user.getCreatedAt());
        
        if (user.getAddress() != null) {
            AddressResponse addressResponse = new AddressResponse();
            addressResponse.setAddressId(user.getAddress().getAddressId());
            addressResponse.setStreet(user.getAddress().getStreet());
            addressResponse.setCity(user.getAddress().getCity());
            addressResponse.setState(user.getAddress().getState());
            addressResponse.setZipCode(user.getAddress().getZipCode());
            addressResponse.setCountry(user.getAddress().getCountry());
            response.setAddress(addressResponse);
        }
        
        return response;
    }
} 