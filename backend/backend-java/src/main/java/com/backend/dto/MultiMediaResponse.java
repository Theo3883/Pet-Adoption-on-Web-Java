package com.backend.dto;

import com.backend.model.MultiMedia;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDate;

@Data
public class MultiMediaResponse {

    private Long id;
    private MultiMedia.MediaType media;

    private String url;

    @JsonProperty("URL")
    public String getURL() {
        return url;
    }

    @JsonProperty("pipeUrl")
    public String getPipeUrl() {
        return url;
    }

    private String description;
    private LocalDate uploadDate;
}