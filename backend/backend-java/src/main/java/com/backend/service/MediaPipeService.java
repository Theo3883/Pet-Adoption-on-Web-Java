package com.backend.service;

import com.backend.exception.ResourceNotFoundException;
import com.backend.model.MultiMedia;
import com.backend.repository.MultiMediaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class MediaPipeService {

    @Autowired
    private MultiMediaRepository multiMediaRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public ResponseEntity<byte[]> getOptimizedMedia(Long id, Integer width, Integer height,
            Integer quality, String format,
            HttpServletRequest request, HttpServletResponse response) {

        MultiMedia media = multiMediaRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.mediaNotFound(id));

        try {
            String filename = extractFilenameFromUrl(media.getUrl());
            String mediaTypeString = media.getMedia().toString();

            byte[] originalData;
            try {
                originalData = fileStorageService.loadFile(mediaTypeString, filename);
            } catch (IOException e) {
                System.out.println("File not found at first attempt: " + mediaTypeString + "/" + filename);
                originalData = findFileByName(filename);
            }

            byte[] optimizedData = processImage(originalData, width, height, quality, format);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaTypeFromFormat(format));
            headers.setContentLength(optimizedData.length);

            headers.setCacheControl("public, max-age=" + TimeUnit.DAYS.toSeconds(30));
            headers.set("ETag", "\"" + id + "-" + width + "x" + height + "-" + quality + "\"");

            String ifNoneMatch = request.getHeader("If-None-Match");
            if (ifNoneMatch != null
                    && ifNoneMatch.equals("\"" + id + "-" + width + "x" + height + "-" + quality + "\"")) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(optimizedData);
        } catch (IOException e) {
            throw new RuntimeException("Error processing image", e);
        }
    }

    public ResponseEntity<byte[]> getOriginalMedia(Long id, HttpServletRequest request, HttpServletResponse response) {
        MultiMedia media = multiMediaRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.mediaNotFound(id));

        try {
            String filename = extractFilenameFromUrl(media.getUrl());
            String mediaTypeString = media.getMedia().toString();

            byte[] originalData;
            try {
                originalData = fileStorageService.loadFile(mediaTypeString, filename);
            } catch (IOException e) {
                System.out.println("File not found at first attempt: " + mediaTypeString + "/" + filename);
                originalData = findFileByName(filename);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaTypeFromMediaType(media.getMedia()));
            headers.setContentLength(originalData.length);

            headers.setCacheControl("public, max-age=" + TimeUnit.DAYS.toSeconds(30));
            headers.set("ETag", "\"original-" + id + "\"");

            String ifNoneMatch = request.getHeader("If-None-Match");
            if (ifNoneMatch != null && ifNoneMatch.equals("\"original-" + id + "\"")) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(originalData);
        } catch (IOException e) {
            throw new RuntimeException("Error loading media file", e);
        }
    }

    private byte[] processImage(byte[] originalData, Integer width, Integer height,
            Integer quality, String format) throws IOException {

        if (width == null && height == null) {
            return originalData;
        }

        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalData));
        if (originalImage == null) {
            return originalData;
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int targetWidth = width != null ? width : originalWidth;
        int targetHeight = height != null ? height : originalHeight;

        if (width != null && height == null) {
            targetHeight = (int) ((double) originalHeight * width / originalWidth);
        } else if (height != null && width == null) {
            targetWidth = (int) ((double) originalWidth * height / originalHeight);
        }

        if (targetWidth > originalWidth || targetHeight > originalHeight) {
            targetWidth = originalWidth;
            targetHeight = originalHeight;
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String outputFormat = format.equalsIgnoreCase("jpg") ? "jpeg" : format;
        ImageIO.write(resizedImage, outputFormat, baos);

        return baos.toByteArray();
    }

    private MediaType getMediaTypeFromFormat(String format) {
        switch (format.toLowerCase()) {
            case "jpeg":
            case "jpg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            case "webp":
                return MediaType.parseMediaType("image/webp");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private MediaType getMediaTypeFromMediaType(MultiMedia.MediaType mediaType) {
        return switch (mediaType) {
            case photo -> MediaType.IMAGE_JPEG;
            case video -> MediaType.parseMediaType("video/mp4");
            case audio -> MediaType.parseMediaType("audio/mpeg");
        };
    }

    private String extractFilenameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private byte[] findFileByName(String filename) throws IOException {
        String[] mediaTypes = { "photo", "video", "audio" };

        for (String mediaType : mediaTypes) {
            try {
                return fileStorageService.loadFile(mediaType, filename);
            } catch (IOException e) {
                System.out.println("File not found in " + mediaType + " directory");
            }
        }
        throw new IOException("File not found in any media directory: " + filename);
    }
}
