package com.backend.service;

import com.backend.dto.*;
import com.backend.dto.AnimalCreationRequest.MedicalHistoryCreationRequest;
import com.backend.dto.AnimalCreationRequest.FeedingScheduleCreationRequest;
import com.backend.dto.AnimalCreationRequest.MultimediaCreationRequest;
import com.backend.dto.AnimalCreationRequest.RelationsCreationRequest;
import com.backend.model.*;
import com.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalService {
    
    private final AnimalRepository animalRepository;
    private final UserService userService;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final FeedingScheduleRepository feedingScheduleRepository;
    private final RelationsRepository relationsRepository;
    private final MultiMediaRepository multiMediaRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Transactional
    public AnimalResponse createBasicAnimal(Long userId, AnimalRequest request) {
        log.info("Creating basic animal for userId: {}", userId);
        
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            log.error("User not found with id: {}", userId);
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        Animal animal = new Animal();
        animal.setName(request.getName());
        animal.setBreed(request.getBreed());
        animal.setSpecies(request.getSpecies());
        animal.setAge(request.getAge());
        animal.setGender(request.getGender());
        animal.setUser(user);
        animal.setViews(0);
        
        Animal savedAnimal = animalRepository.save(animal);
        log.info("Animal created successfully with ID: {}", savedAnimal.getAnimalId());
        
        return convertToAnimalResponse(savedAnimal);
    }
    
    @Transactional
    public void addMedicalHistory(Long animalId, List<MedicalHistoryCreationRequest> medicalHistoryRequests) {
        log.info("Adding medical history for animalId: {}", animalId);
        
        Optional<Animal> animalOpt = animalRepository.findById(animalId);
        if (animalOpt.isEmpty()) {
            log.error("Animal not found with ID: {}", animalId);
            throw new RuntimeException("Animal not found with ID: " + animalId);
        }
        
        Animal animal = animalOpt.get();
        
        if (medicalHistoryRequests != null && !medicalHistoryRequests.isEmpty()) {
            for (MedicalHistoryCreationRequest request : medicalHistoryRequests) {
                MedicalHistory medicalHistory = new MedicalHistory();
                medicalHistory.setAnimal(animal);
                medicalHistory.setVetNumber(request.getVetNumber());
                medicalHistory.setDescription(request.getDescription());
                medicalHistory.setFirstAidNoted(request.getFirst_aid_noted());
                
                if (request.getRecordDate() != null && !request.getRecordDate().isEmpty()) {
                    try {
                        medicalHistory.setRecordDate(LocalDate.parse(request.getRecordDate()));
                    } catch (Exception e) {
                        log.warn("Failed to parse record date: {}, using current date", request.getRecordDate());
                        medicalHistory.setRecordDate(LocalDate.now());
                    }
                } else {
                    medicalHistory.setRecordDate(LocalDate.now());
                }
                
                medicalHistoryRepository.save(medicalHistory);
            }
            log.info("Added {} medical history records for animal ID: {}", medicalHistoryRequests.size(), animalId);
        }
    }
    
    @Transactional
    public void addFeedingSchedule(Long animalId, List<FeedingScheduleCreationRequest> feedingScheduleRequests) {
        log.info("Adding feeding schedule for animalId: {}", animalId);
        
        Optional<Animal> animalOpt = animalRepository.findById(animalId);
        if (animalOpt.isEmpty()) {
            log.error("Animal not found with ID: {}", animalId);
            throw new RuntimeException("Animal not found with ID: " + animalId);
        }
        
        if (feedingScheduleRequests != null && !feedingScheduleRequests.isEmpty()) {
            // Only create one feeding schedule per animal (as per database constraint)
            FeedingScheduleCreationRequest request = feedingScheduleRequests.get(0);
            
            // Use custom method to insert with proper VARRAY handling
            insertFeedingScheduleWithVArray(animalId, request.getFeedingTimes(), request.getFoodType(), request.getNotes());
            log.info("Added feeding schedule for animal ID: {}", animalId);
        }
    }
    
    @Transactional
    public void insertFeedingScheduleWithVArray(Long animalId, List<String> feedingTimes, String foodType, String notes) {
        if (feedingTimes == null || feedingTimes.isEmpty()) {
            log.warn("No feeding times provided for animal ID: {}", animalId);
            return;
        }
        
        Optional<Animal> animalOpt = animalRepository.findById(animalId);
        if (animalOpt.isEmpty()) {
            log.error("Animal not found with ID: {}", animalId);
            throw new RuntimeException("Animal not found with ID: " + animalId);
        }
        
        StringBuilder varrayConstructor = new StringBuilder("feeding_time_array(");
        boolean hasValidTimes = false;
        
        for (int i = 0; i < feedingTimes.size() && i < 10; i++) { 
            String timeStr = feedingTimes.get(i);
            
            // Convert HH:MM to HH:MM:SS format if needed
            String formattedTime = normalizeTimeFormat(timeStr);
            
            if (formattedTime != null && !formattedTime.isEmpty()) {
                if (hasValidTimes) {
                    varrayConstructor.append(", ");
                }
                // Use VARCHAR2 format since the VARRAY is defined as VARCHAR2(50)
                varrayConstructor.append("'").append(formattedTime).append("'");
                hasValidTimes = true;
            }
        }
        
        varrayConstructor.append(")");
        
        if (!hasValidTimes) {
            log.warn("No valid feeding times found for animal ID: {}", animalId);
            return;
        }
        
        String sql = "INSERT INTO FeedingSchedule (animalID, feeding_time, food_type, notes) " +
                     "VALUES (?, " + varrayConstructor.toString() + ", ?, ?)";
        
        log.debug("Executing SQL: {}", sql);
        log.debug("Parameters: animalId={}, foodType={}, notes={}", animalId, foodType, notes);
        
        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, animalId);
            query.setParameter(2, foodType);
            query.setParameter(3, notes);
            
            int result = query.executeUpdate();
            log.info("Feeding schedule inserted successfully for animal ID: {}, rows affected: {}", animalId, result);
        } catch (Exception e) {
            log.error("Error inserting feeding schedule for animal ID {}: {}", animalId, e.getMessage(), e);
            throw new RuntimeException("Failed to insert feeding schedule: " + e.getMessage(), e);
        }
    }
    
    private String normalizeTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        timeStr = timeStr.trim();
        
        // If already in HH:MM:SS format, validate and return
        if (timeStr.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return timeStr;
        }
        
        // If in HH:MM format, convert to HH:MM:SS
        if (timeStr.matches("\\d{1,2}:\\d{2}")) {
            return timeStr + ":00";
        }
        
        // Invalid format
        return null;
    }
    
    @Transactional
    public void addMultimedia(Long animalId, List<MultimediaCreationRequest> multimediaRequests) {
        log.debug("Adding multimedia for animalId: {}", animalId);
        log.debug("Multimedia requests count: {}", multimediaRequests != null ? multimediaRequests.size() : 0);
        
        Optional<Animal> animalOpt = animalRepository.findById(animalId);
        if (animalOpt.isEmpty()) {
            log.error("Animal not found with ID: {}", animalId);
            throw new RuntimeException("Animal not found with ID: " + animalId);
        }
        
        Animal animal = animalOpt.get();
        log.debug("Found animal: {}", animal.getName());
        
        if (multimediaRequests != null && !multimediaRequests.isEmpty()) {
            for (MultimediaCreationRequest request : multimediaRequests) {
                log.debug("Processing multimedia: mediaType={}, url={}, description={}", 
                         request.getMediaType(), request.getUrl(), request.getDescription());
                
                MultiMedia multiMedia = new MultiMedia();
                multiMedia.setAnimal(animal);
                multiMedia.setUrl(request.getUrl());
                multiMedia.setDescription(request.getDescription());
                multiMedia.setUploadDate(LocalDate.now());
                
                try {
                    multiMedia.setMedia(MultiMedia.MediaType.valueOf(request.getMediaType().toLowerCase()));
                    log.debug("Set media type: {}", request.getMediaType());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid media type: {}, defaulting to photo", request.getMediaType());
                    multiMedia.setMedia(MultiMedia.MediaType.photo);
                }
                
                MultiMedia saved = multiMediaRepository.save(multiMedia);
                log.debug("Saved multimedia with ID: {}", saved.getId());
            }
            log.info("Added {} multimedia records for animal ID: {}", multimediaRequests.size(), animalId);
        } else {
            log.debug("No multimedia data to add for animal ID: {}", animalId);
        }
    }
    
    @Transactional
    public void addRelations(Long animalId, RelationsCreationRequest relationsRequest) {
        log.debug("Adding relations for animalId: {}", animalId);
        log.debug("Relations request: {}", relationsRequest != null ? relationsRequest.getFriendWith() : "null");
        
        Optional<Animal> animalOpt = animalRepository.findById(animalId);
        if (animalOpt.isEmpty()) {
            log.error("Animal not found with ID: {}", animalId);
            throw new RuntimeException("Animal not found with ID: " + animalId);
        }
        
        Animal animal = animalOpt.get();
        log.debug("Found animal: {}", animal.getName());
        
        if (relationsRequest != null && relationsRequest.getFriendWith() != null && !relationsRequest.getFriendWith().isEmpty()) {
            Relations relations = new Relations();
            relations.setAnimal(animal);
            relations.setFriendWith(relationsRequest.getFriendWith());
            
            Relations saved = relationsRepository.save(relations);
            log.info("Added relations with ID: {} for animal ID: {}", saved.getId(), animalId);
        } else {
            log.debug("No relations data to add for animal ID: {}", animalId);
        }
    }

    public List<AnimalResponse> getAllAnimals() {
        return animalRepository.findAll()
                .stream()
                .map(this::convertToAnimalResponse)
                .collect(Collectors.toList());
    }
    
    public Optional<AnimalResponse> getAnimalById(Long animalId) {
        return animalRepository.findById(animalId)
                .map(this::convertToAnimalResponse);
    }
    
    public Optional<AnimalDetailResponse> getAnimalDetailById(Long animalId) {
        Optional<Animal> animalOpt = animalRepository.findById(animalId);
        if (animalOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Animal animal = animalOpt.get();
        
        List<MultiMedia> multimedia = multiMediaRepository.findByAnimalAnimalId(animalId);
        List<MedicalHistory> medicalHistories = medicalHistoryRepository.findByAnimalAnimalId(animalId);
        
        List<Object[]> feedingScheduleData = List.of();
        try {
            feedingScheduleData = feedingScheduleRepository.findFeedingScheduleWithExtractedTimes(animalId);
        } catch (Exception e) {
            log.warn("Failed to retrieve feeding schedule with VARRAY extraction for animal {}, using fallback", animalId);
            // Fallback: use simple repository method
            try {
                Optional<FeedingSchedule> feedingScheduleOpt = feedingScheduleRepository.findByAnimalAnimalId(animalId);
                if (feedingScheduleOpt.isPresent()) {
                    FeedingSchedule fs = feedingScheduleOpt.get();
                    // Create a mock Object[] array to match the expected format
                    Object[] mockData = new Object[]{
                        fs.getId(),
                        animalId,
                        fs.getFeedingTime(), // This will be the raw string
                        fs.getFoodType(),
                        fs.getNotes()
                    };
                    feedingScheduleData = new ArrayList<>();
                    feedingScheduleData.add(mockData);
                }
            } catch (Exception fallbackException) {
                log.error("Both feeding schedule retrieval methods failed for animal {}", animalId);
                feedingScheduleData = List.of();
            }
        }
        
        Optional<Relations> relations = relationsRepository.findByAnimalAnimalId(animalId);
        
        AnimalDetailResponse response = new AnimalDetailResponse();
        response.setAnimal(convertToAnimalResponse(animal));
        
        response.setMultimedia(multimedia.stream()
                .map(this::convertToMultiMediaResponse)
                .collect(Collectors.toList()));
        
        response.setMedicalHistory(medicalHistories.stream()
                .map(this::convertToMedicalHistoryResponse)
                .collect(Collectors.toList()));
        
        if (!feedingScheduleData.isEmpty()) {
            Object[] data = feedingScheduleData.get(0);
            FeedingScheduleResponse feedingResponse = new FeedingScheduleResponse();
            feedingResponse.setId(((Number) data[0]).longValue());
            
            String feedingTimesString = (String) data[2];
            if (feedingTimesString != null && !feedingTimesString.isEmpty()) {
                List<String> feedingTimes = Arrays.stream(feedingTimesString.split(","))
                        .map(String::trim)
                        .map(this::extractTimeFromTimestamp)
                        .filter(time -> !time.isEmpty())
                        .collect(Collectors.toList());
                feedingResponse.setFeedingTime(feedingTimes);
            } else {
                feedingResponse.setFeedingTime(List.of());
            }
            
            feedingResponse.setFoodType((String) data[3]);
            feedingResponse.setNotes((String) data[4]);
            
            response.setFeedingSchedule(List.of(feedingResponse));
        } else {
            response.setFeedingSchedule(List.of());
        }
        
        if (relations.isPresent()) {
            List<RelationsResponse> relationsList = List.of(convertToRelationsResponse(relations.get()));
            response.setRelations(relationsList);
        } else {
            response.setRelations(List.of());
        }
        
        if (animal.getUser() != null) {
            response.setOwner(convertToUserResponse(animal.getUser()));
            if (animal.getUser().getAddress() != null) {
                // Convert address to List
                List<AddressResponse> addressList = List.of(convertToAddressResponse(animal.getUser().getAddress()));
                response.setAddress(addressList);
            } else {
                response.setAddress(List.of());
            }
        }
        
        return Optional.of(response);
    }
    
    public List<AnimalResponse> getAnimalsBySpecies(String species) {
        return animalRepository.findBySpecies(species)
                .stream()
                .map(this::convertToAnimalResponse)
                .collect(Collectors.toList());
    }
    
    public void incrementViews(Long animalId) {
        animalRepository.incrementViews(animalId);
    }
    
    public void deleteAnimal(Long animalId) {
        if (!animalRepository.existsById(animalId)) {
            throw new RuntimeException("Animal not found");
        }
        animalRepository.deleteById(animalId);
    }
    
    public List<AnimalResponse> getTopAnimalsByUserCity(Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().getAddress() == null) {
            return List.of();
        }
        
        String city = userOpt.get().getAddress().getCity();
        return animalRepository.findTopAnimalsByCity(city)
                .stream()
                .map(this::convertToAnimalResponse)
                .collect(Collectors.toList());
    }
    
    public List<Object[]> getPopularBreedsBySpecies(String species) {
        return animalRepository.findPopularBreedsBySpecies(species);
    }
    
    private AnimalResponse convertToAnimalResponse(Animal animal) {
        AnimalResponse response = new AnimalResponse();
        response.setAnimalId(animal.getAnimalId());
        response.setName(animal.getName());
        response.setBreed(animal.getBreed());
        response.setSpecies(animal.getSpecies());
        response.setAge(animal.getAge());
        response.setViews(animal.getViews());
        response.setGender(animal.getGender());
        response.setCreatedAt(animal.getCreatedAt());
        
        if (animal.getUser() != null) {
            response.setUserId(animal.getUser().getUserId());
            response.setUser(convertToUserResponse(animal.getUser()));
        }

        if (animal.getMultimedia() != null) {
            List<MultiMediaResponse> multimediaResponses = animal.getMultimedia().stream()
                    .map(this::convertToMultiMediaResponse)
                    .collect(Collectors.toList());
            response.setMultimedia(multimediaResponses);
        }
        
        return response;
    }
    
    private UserResponse convertToUserResponse(User user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(user.getUserId());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setEmail(user.getEmail());
        userResponse.setPhone(user.getPhone());
        userResponse.setCreatedAt(user.getCreatedAt());
        return userResponse;
    }
    
    private AddressResponse convertToAddressResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setAddressId(address.getAddressId());
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setZipCode(address.getZipCode());
        response.setCountry(address.getCountry());
        return response;
    }
    
    private MultiMediaResponse convertToMultiMediaResponse(MultiMedia multiMedia) {
        MultiMediaResponse response = new MultiMediaResponse();
        response.setId(multiMedia.getId());
        response.setMedia(multiMedia.getMedia());
        response.setUrl(multiMedia.getUrl());
        response.setDescription(multiMedia.getDescription());
        response.setUploadDate(multiMedia.getUploadDate());
        return response;
    }
    
    private MedicalHistoryResponse convertToMedicalHistoryResponse(MedicalHistory medicalHistory) {
        MedicalHistoryResponse response = new MedicalHistoryResponse();
        response.setId(medicalHistory.getId());
        response.setVetNumber(medicalHistory.getVetNumber());
        response.setRecordDate(medicalHistory.getRecordDate());
        response.setDescription(medicalHistory.getDescription());
        response.setFirstAidNoted(medicalHistory.getFirstAidNoted());
        return response;
    }
    
    private RelationsResponse convertToRelationsResponse(Relations relations) {
        RelationsResponse response = new RelationsResponse();
        response.setId(relations.getId());
        response.setFriendWith(relations.getFriendWith());
        return response;
    }
    
    private String extractTimeFromTimestamp(String timestampOrTime) {
        if (timestampOrTime == null || timestampOrTime.isEmpty()) {
            return "";
        }
        
        // If it's already in HH:MM:SS format, return as is
        if (timestampOrTime.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return timestampOrTime;
        }
        
        // Handle Oracle timestamp format: "01/05/25 07:30:00.000000000"
        if (timestampOrTime.contains(" ")) {
            String[] parts = timestampOrTime.split(" ");
            if (parts.length > 1) {
                String timePart = parts[1];
                // Extract HH:MM:SS from HH:MM:SS.nnnnnnnnn
                if (timePart.contains(".")) {
                    timePart = timePart.substring(0, timePart.indexOf("."));
                }
                if (timePart.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
                    return timePart;
                }
            }
        }
        
        String timePattern = timestampOrTime.replaceAll(".*?(\\d{1,2}:\\d{2}:\\d{2}).*", "$1");
        if (timePattern.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return timePattern;
        }
        
        return timestampOrTime; 
    }
} 