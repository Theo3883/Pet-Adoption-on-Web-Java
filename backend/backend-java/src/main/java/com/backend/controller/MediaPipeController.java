package com.backend.controller;

import com.backend.service.MediaPipeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/media")
@CrossOrigin(origins = "*")
public class MediaPipeController {

    @Autowired
    private MediaPipeService mediaPipeService;

    @GetMapping("/pipe/{id}")
    public ResponseEntity<byte[]> getOptimizedMedia(
            @PathVariable Long id,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height,
            @RequestParam(value = "quality", required = false, defaultValue = "85") Integer quality,
            @RequestParam(value = "format", required = false, defaultValue = "jpeg") String format,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        return mediaPipeService.getOptimizedMedia(id, width, height, quality, format, request, response);
    }

    @GetMapping("/original/{id}")
    public ResponseEntity<byte[]> getOriginalMedia(
            @PathVariable Long id,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        return mediaPipeService.getOriginalMedia(id, request, response);
    }
}
