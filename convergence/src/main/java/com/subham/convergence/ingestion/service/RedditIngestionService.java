package com.subham.convergence.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.common.IngestionState;
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

    @Scheduled(fixedDelay = 3600000) // every hour)(unit in ms)
    private void fetch() {
        if(redditUrl == null || redditUrl.isEmpty())
        {
            log.error("[Reddit] Reddit URL is not configured. Please set the 'reddit.url' property.");
            return;
        }

        log.info("[Reddit] fetching bookmarks from {}", redditUrl);

        try{
            //get last id
            IngestionState state = ingestionStateRepository.findByPlatormType(PlatformType.REDDIT)
                    .orElse(new IngestionState(PlatformType.REDDIT, null, 0L));
            
            String lastProcessedId = state.getLastProcessedId();
            String newLatestId = null;
            //fetch json
            JsonNode root = objectMapper.readTree(URI.create(redditUrl).toURL());
            JsonNode children = root.path("data").path("children");
            
            List<IngestRequest> extractedBoookmarks = new ArrayList<>();

            for(JsonNode child : children)
            {
                String kind= child.path("kind").asText();
                JsonNode data = child.path("data");
                String id = data.path("id").asText();
                if(newLatestId == null)
                {
                    newLatestId = id;
                }
                if(id.equals(lastProcessedId))
                {
                    log.info("[Reddit] Reached already processed ID: stopped");
                    break; //already processed this and all the rest, so break
                }

                //map json to ingestrequest dto
                IngestRequest request = new IngestRequest();
                request.setPlatformType(PlatformType.REDDIT);
                request.setExternalId(id);
                request.setUrl("https://reddit.com" + data.path("permalink").asText() );

                //populating metadata
                Map<String,Object> metadata = new HashMap<>();
                metadata.put("author", data.path("author").asText());
                metadata.put("subreddit", data.path("subreddit").asText());
                metadata.put("score",data.path("score").asInt());
                metadata.put("created_utc", data.path("created_utc").asLong());
                metadata.put("type", kind);

                //t1 or t3
                if ("t3".equals(kind)) {
                    request.setTitle(data.path("title").asText());
                    request.setDescription(data.path("selftext").asText()); 
                    
                    String thumbnail = data.path("thumbnail").asText();
                    if (thumbnail != null && thumbnail.startsWith("http")) {
                        request.setThumbnailUrl(thumbnail);
                    }
                    
                    // --- Image Extraction Logic ---
                    List<String> images = new ArrayList<>();
                    
                    if (data.path("is_gallery").asBoolean(false)) {
                        JsonNode galleryItems = data.path("gallery_data").path("items");
                        JsonNode mediaMetadata = data.path("media_metadata");
                        
                        if (galleryItems.isArray()) {
                            for (JsonNode item : galleryItems) {
                                String mediaId = item.path("media_id").asText();
                                String imageUrl = mediaMetadata.path(mediaId).path("s").path("u").asText();
                                
                                if (!imageUrl.isEmpty()) {
                                    // Reddit escapes '&' as '&amp;'. Must unescape for valid links.
                                    images.add(imageUrl.replace("&amp;", "&"));
                                }
                            }
                        }
                    } else {
                        String postUrl = data.path("url").asText();
                        if (postUrl.matches("(?i).*\\.(jpg|jpeg|png|gif|webp)$")) {
                            images.add(postUrl);
                        }
                    }
                    
                    // Put the images list into metadata if it is not empty
                    if (!images.isEmpty()) {
                        metadata.put("imageUrls", images);
                    }
                    // ------------------------------
                    
                } else if ("t1".equals(kind)) {
                    request.setTitle("Comment on: " + data.path("link_title").asText());
                    request.setDescription(data.path("body").asText()); 
                } else {
                    log.warn("[Reddit] Unknown kind type '{}' for ID: {}", kind, currentId);
                    continue; 
                }

                request.setMetadata(metadata);
                extractedBookmarks.add(request);
            }
            log.info("[Reddit] bookmarks extracted: {}", extractedBookmarks.size());
            //save to db or json
            
            saveBookmarkstoFile(extractedBookmarks, "reddit_bookmarks.json");

            //update lastprocessedId
            if(newLatestId != null)
            {
                state.setLastProcessedId(newLatestId);
                state.setLastProcessedTimestamp(System.currentTimeMillis());
                ingestionStateRepository.save(state);
                log.info("[Reddit] Updated last processed ID to: {}", newLatestId);
            }
        }catch(Exception e)
        {
            log.error("[Reddit] Failed to fetch bookmarks: {}", e.getMessage());
        }
    }
    private void saveBookmarkstoFile(List<IngestRequest> bookmarks, String filename) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), bookmarks);
            log.info("[Reddit] Bookmarks saved to file: {}", filename);
        } catch (IOException e) {
            log.error("[Reddit] Failed to save bookmarks to file: {}", e.getMessage());
        }
    }
}

