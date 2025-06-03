package com.backend.controller;

import com.backend.dto.*;
import com.backend.dto.AnimalCreationRequest.MedicalHistoryCreationRequest;
import com.backend.dto.AnimalCreationRequest.FeedingScheduleCreationRequest;
import com.backend.dto.AnimalCreationRequest.MultimediaCreationRequest;
import com.backend.dto.AnimalCreationRequest.RelationsCreationRequest;
import com.backend.service.AnimalService;
import com.backend.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AnimalController {
    
    private final AnimalService animalService;
    private final JwtService jwtService;
      
    @GetMapping("/animals/all")
    public ResponseEntity<List<AnimalResponse>> getAllAnimals(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }
        
        List<AnimalResponse> animals = animalService.getAllAnimals();
        return ResponseEntity.ok(animals);
    }   
    
    @PostMapping("/animals/details")
    public ResponseEntity<AnimalDetailResponse> getAnimalDetailsById(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        log.debug("Received animal details request: {}", request);
        
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            log.warn("Authentication failed for animal details request");
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }
        
        log.debug("User authenticated: {}", userId);
        
        if (request.get("animalId") == null) {
            log.warn("Missing animalId in request: {}", request);
            throw new com.backend.exception.ValidationException("animalId is required");
        }
        
        Long animalId;
        try {
            animalId = Long.valueOf(request.get("animalId").toString());
        } catch (NumberFormatException e) {
            log.error("Invalid animalId format in request: {}", request, e);
            throw com.backend.exception.ValidationException.invalidFormat("animalId");
        }
        
        log.info("Getting details for animal ID: {} requested by user ID: {}", animalId, userId);
        
        try {
            animalService.incrementViews(animalId);
            log.debug("Views incremented for animal ID: {}", animalId);
        } catch (Exception e) {
            log.warn("Failed to increment views for animal ID {}: {}", animalId, e.getMessage());
        }
        
        Optional<AnimalDetailResponse> animalDetail = animalService.getAnimalDetailById(animalId);
        if (animalDetail.isPresent()) {
            log.info("Successfully retrieved details for animal ID: {}", animalId);
            return ResponseEntity.ok(animalDetail.get());
        } else {
            log.warn("Animal not found with ID: {}", animalId);
            throw com.backend.exception.ResourceNotFoundException.animalNotFound(animalId);
        }
    }    
    
    @PostMapping("/animals/species")
    public ResponseEntity<Map<String, Object>> getAnimalsBySpecies(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }
        
        String species = request.get("species");
        if (species == null) {
            throw com.backend.exception.ValidationException.missingRequiredField("species");
        }
        
        List<AnimalResponse> animals = animalService.getAnimalsBySpecies(species);
        List<Object[]> popularBreeds = animalService.getPopularBreedsBySpecies(species);
        
        Map<String, Object> response = new HashMap<>();
        response.put("animals", animals);
        response.put("popularBreeds", popularBreeds);
        
        return ResponseEntity.ok(response);
    }   
    
    @PostMapping("/animals/create")
    public ResponseEntity<AnimalResponse> createAnimal(@Valid @RequestBody AnimalCreationRequest request, HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }

        log.info("Creating basic animal for user ID: {}", userId);
        
        AnimalRequest basicRequest = new AnimalRequest();
        basicRequest.setName(request.getName());
        basicRequest.setBreed(request.getBreed());
        basicRequest.setSpecies(request.getSpecies());
        basicRequest.setAge(request.getAge());
        basicRequest.setGender(request.getGender());
        
        AnimalResponse animal = animalService.createBasicAnimal(userId, basicRequest);
        
        log.info("Basic animal created successfully with ID: {}", animal.getAnimalId());
        
        return ResponseEntity.ok(animal);
    }    
    
    @DeleteMapping("/animals/delete")
    public ResponseEntity<Map<String, String>> deleteAnimal(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }
        
        if (request.get("animalId") == null) {
            throw com.backend.exception.ValidationException.missingRequiredField("animalId");
        }
        
        long animalId;
        try {
            animalId = Long.parseLong(request.get("animalId").toString());
        } catch (NumberFormatException e) {
            throw com.backend.exception.ValidationException.invalidFormat("animalId");
        }
        
        animalService.deleteAnimal(animalId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Animal deleted successfully");
        return ResponseEntity.ok(response);
    }    
    
    @GetMapping("/animals/top-by-city")
    public ResponseEntity<List<AnimalResponse>> getTopAnimalsByCity(@RequestParam Long userId, HttpServletRequest httpRequest) {
        // Check if user is authenticated
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.backend.exception.AuthorizationException("Access denied. Please log in.");
        }

        String token = authHeader.substring(7);
        Long tokenUserId = jwtService.extractUserId(token);
        if (tokenUserId == null) {
            throw new com.backend.exception.AuthenticationException("Invalid token");
        }

        List<AnimalResponse> animals = animalService.getTopAnimalsByUserCity(userId);
        return ResponseEntity.ok(animals);
    }
    
    private Long extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtService.extractUserId(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }    
    
    @PostMapping("/animals/{animalId}/medical-history")
    public ResponseEntity<Map<String, String>> addMedicalHistory(@PathVariable Long animalId, 
                                             @Valid @RequestBody List<MedicalHistoryCreationRequest> medicalHistoryRequests,
                                             HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }

        log.info("Adding medical history for animal ID: {} by user ID: {}", animalId, userId);
        animalService.addMedicalHistory(animalId, medicalHistoryRequests);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Medical history added successfully");
        return ResponseEntity.ok(response);
    }   
    
    @PostMapping("/animals/{animalId}/feeding-schedule")
    public ResponseEntity<Map<String, String>> addFeedingSchedule(@PathVariable Long animalId,
                                              @Valid @RequestBody List<FeedingScheduleCreationRequest> feedingScheduleRequests,
                                              HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }        log.info("Adding feeding schedule for animal ID: {} by user ID: {}", animalId, userId);
        animalService.addFeedingSchedule(animalId, feedingScheduleRequests);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Feeding schedule added successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/animals/{animalId}/multimedia")
    public ResponseEntity<Map<String, String>> addMultimedia(@PathVariable Long animalId,
                                         @Valid @RequestBody List<MultimediaCreationRequest> multimediaRequests,
                                         HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }

        log.info("Adding multimedia for animal ID: {} by user ID: {}", animalId, userId);
        animalService.addMultimedia(animalId, multimediaRequests);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Multimedia added successfully");
        return ResponseEntity.ok(response);
    }    
    
    @PostMapping("/animals/{animalId}/relations")
    public ResponseEntity<Map<String, String>> addRelations(@PathVariable Long animalId,
                                        @Valid @RequestBody RelationsCreationRequest relationsRequest,
                                        HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromToken(httpRequest);
        if (userId == null) {
            throw new com.backend.exception.AuthenticationException("Authentication required");
        }

        log.info("Adding relations for animal ID: {} by user ID: {}", animalId, userId);
        animalService.addRelations(animalId, relationsRequest);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Relations added successfully");
        return ResponseEntity.ok(response);
    }
}