package com.subham.convergence.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.dto.IngestRequest;
import com.subham.convergence.ingestion.repository.IngestionStateRepository;

//need to add scheduling
@Service
@Slf4j
@RequiredArgsConstructor
public class RedditIngestionService {
    private final IngestionState ingestionState;
    private final IngestionStateRepository ingestionStateRepository;
    private final ObjectMapper objectMapper;

    @Value("${reddit.url}"
    private String redditUrl;

    private void 

}
