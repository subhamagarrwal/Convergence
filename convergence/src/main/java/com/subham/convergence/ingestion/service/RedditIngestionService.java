package com.subham.convergence.ingestion.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.subham.convergence.ingestion.dto.NormalizedBookmark;
import com.subham.convergence.ingestion.repository.IngestionStateRepository;
import com.subham.convergence.ingestion.common.IngestionState;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Comparator;

@Service
public class RedditIngestionService {    
    @Value("${reddit.url}")
    private String redditApiUrl;


}
