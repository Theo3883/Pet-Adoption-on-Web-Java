package com.backend.service;

import com.backend.model.Newsletter;
import com.backend.model.User;
import com.backend.repository.NewsletterRepository;
import com.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NewsletterService {
    
    private final NewsletterRepository newsletterRepository;
    private final UserRepository userRepository;
    
    public List<Map<String, Object>> getSubscriptions(Long userId) {
        List<Newsletter> subscriptions = newsletterRepository.findByUserUserId(userId);
        
        return subscriptions.stream().map(newsletter -> {
            Map<String, Object> subscriptionMap = new HashMap<>();
            subscriptionMap.put("ID", newsletter.getId());
            subscriptionMap.put("USERID", newsletter.getUser().getUserId());
            subscriptionMap.put("SPECIES", newsletter.getSpecies());
            subscriptionMap.put("ISACTIVE", newsletter.getIsActive() ? 1 : 0);
            subscriptionMap.put("SUBSCRIBEDAT", newsletter.getSubscribedAt());
            return subscriptionMap;
        }).collect(Collectors.toList());
    }
    
    public void updateSubscriptions(Long userId, List<String> speciesList) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        List<Newsletter> existingSubscriptions = newsletterRepository.findByUserUserId(userId);
        for (Newsletter existing : existingSubscriptions) {
            if (!speciesList.contains(existing.getSpecies())) {
                newsletterRepository.delete(existing);
            }
        }
        
        for (String species : speciesList) {
            if (!newsletterRepository.existsByUserUserIdAndSpecies(userId, species)) {
                Newsletter newsletter = new Newsletter();
                newsletter.setUser(user);
                newsletter.setSpecies(species);
                newsletter.setIsActive(true);
                newsletterRepository.save(newsletter);
            }
        }
    }
    
    public List<User> getSubscribersBySpecies(String species) {
        List<Newsletter> subscriptions = newsletterRepository.findActiveSubscribersBySpecies(species);
        return subscriptions.stream()
                .map(Newsletter::getUser)
                .collect(Collectors.toList());
    }
} 