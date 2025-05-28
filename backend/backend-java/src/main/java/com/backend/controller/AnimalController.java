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
    public ResponseEntity<?> getAllAnimals(HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            List<AnimalResponse> animals = animalService.getAllAnimals();
            return ResponseEntity.ok(animals);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/animals/details")
    public ResponseEntity<?> getAnimalDetailsById(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            log.debug("Received animal details request: {}", request);
            
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                log.warn("Authentication failed for animal details request");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            log.debug("User authenticated: {}", userId);
            
            if (request.get("animalId") == null) {
                log.warn("Missing animalId in request: {}", request);
                Map<String, String> error = new HashMap<>();
                error.put("error", "animalId is required");
                return ResponseEntity.status(400).body(error);
            }
            
            Long animalId = Long.valueOf(request.get("animalId").toString());
            log.info("Getting details for animal ID: {} requested by user ID: {}", animalId, userId);
            
            try {
                animalService.incrementViews(animalId);
                log.debug("Views incremented for animal ID: {}", animalId);
            } catch (Exception e) {
                log.warn("Failed to increment views for animal ID {}: {}", animalId, e.getMessage());
            }
            
            // Get detailed animal information
            Optional<AnimalDetailResponse> animalDetail = animalService.getAnimalDetailById(animalId);
            if (animalDetail.isPresent()) {
                log.info("Successfully retrieved details for animal ID: {}", animalId);
                return ResponseEntity.ok(animalDetail.get());
            } else {
                log.warn("Animal not found with ID: {}", animalId);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Animal not found");
                return ResponseEntity.status(404).body(error);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid animalId format in request: {}", request, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid animalId format");
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            log.error("Error getting animal details for request {}: {}", request, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/animals/species")
    public ResponseEntity<?> getAnimalsBySpecies(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String species = request.get("species");
            if (species == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Species is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<AnimalResponse> animals = animalService.getAnimalsBySpecies(species);
            List<Object[]> popularBreeds = animalService.getPopularBreedsBySpecies(species);
            
            Map<String, Object> response = new HashMap<>();
            response.put("animals", animals);
            response.put("popularBreeds", popularBreeds);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/animals/create")
    public ResponseEntity<?> createAnimal(@RequestBody AnimalCreationRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }

            log.info("Creating basic animal for user ID: {}", userId);
            
            // Create only basic animal info
            AnimalRequest basicRequest = new AnimalRequest();
            basicRequest.setName(request.getName());
            basicRequest.setBreed(request.getBreed());
            basicRequest.setSpecies(request.getSpecies());
            basicRequest.setAge(request.getAge());
            basicRequest.setGender(request.getGender());
            
            AnimalResponse animal = animalService.createBasicAnimal(userId, basicRequest);
            
            log.info("Basic animal created successfully with ID: {}", animal.getAnimalId());
            
            return ResponseEntity.ok(animal);
        } catch (Exception e) {
            log.error("Error creating basic animal: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/animals/delete")
    public ResponseEntity<?> deleteAnimal(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            Long animalId = Long.valueOf(request.get("animalId").toString());
            
            animalService.deleteAnimal(animalId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Animal deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/animals/top-by-city")
    public ResponseEntity<?> getTopAnimalsByCity(@RequestParam Long userId, HttpServletRequest httpRequest) {
        try {
            // Check if user is authenticated
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Access denied. Please log in.");
                return ResponseEntity.status(403).body(error);
            }

            String token = authHeader.substring(7);
            Long tokenUserId = jwtService.extractUserId(token);
            if (tokenUserId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid token");
                return ResponseEntity.status(401).body(error);
            }

            List<AnimalResponse> animals = animalService.getTopAnimalsByUserCity(userId);
            return ResponseEntity.ok(animals);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            return ResponseEntity.status(500).body(error);
        }
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
    public ResponseEntity<?> addMedicalHistory(@PathVariable Long animalId, 
                                             @RequestBody List<MedicalHistoryCreationRequest> medicalHistoryRequests,
                                             HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }

            log.info("Adding medical history for animal ID: {} by user ID: {}", animalId, userId);
            animalService.addMedicalHistory(animalId, medicalHistoryRequests);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Medical history added successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding medical history for animal ID {}: {}", animalId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/animals/{animalId}/feeding-schedule")
    public ResponseEntity<?> addFeedingSchedule(@PathVariable Long animalId,
                                              @RequestBody List<FeedingScheduleCreationRequest> feedingScheduleRequests,
                                              HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }

            log.info("Adding feeding schedule for animal ID: {} by user ID: {}", animalId, userId);
            animalService.addFeedingSchedule(animalId, feedingScheduleRequests);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Feeding schedule added successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding feeding schedule for animal ID {}: {}", animalId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/animals/{animalId}/multimedia")
    public ResponseEntity<?> addMultimedia(@PathVariable Long animalId,
                                         @RequestBody List<MultimediaCreationRequest> multimediaRequests,
                                         HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }

            log.info("Adding multimedia for animal ID: {} by user ID: {}", animalId, userId);
            animalService.addMultimedia(animalId, multimediaRequests);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Multimedia added successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding multimedia for animal ID {}: {}", animalId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/animals/{animalId}/relations")
    public ResponseEntity<?> addRelations(@PathVariable Long animalId,
                                        @RequestBody RelationsCreationRequest relationsRequest,
                                        HttpServletRequest httpRequest) {
        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }

            log.info("Adding relations for animal ID: {} by user ID: {}", animalId, userId);
            animalService.addRelations(animalId, relationsRequest);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Relations added successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding relations for animal ID {}: {}", animalId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
} 