package com.subham.convergence.ingestion.service;

import com.subham.convergence.enums.ContentType;
import com.subham.convergence.enums.PlatformType;
import com.subham.convergence.ingestion.dto.YouTubeExtensionPayload;
import com.subham.convergence.ingestion.dto.YouTubeExtensionPayload.VideoItem;
import com.subham.convergence.ingestion.dto.YouTubeSyncResponse;
import com.subham.convergence.model.Bookmark;
import com.subham.convergence.model.User;
import com.subham.convergence.repository.BookmarkRepository;
import com.subham.convergence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeExtensionProcessor {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    @Transactional
    public YouTubeSyncResponse processAndSave(YouTubeExtensionPayload payload) {
        UUID userId = UUID.fromString(payload.getUserId());

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return YouTubeSyncResponse.builder()
                    .status("error")
                    .message("User not found: " + payload.getUserId())
                    .build();
        }

        User user = userOpt.get();
        int newCount = 0;

        for (VideoItem video : payload.getVideos()) {
            if (video.getVideoId() == null || video.getVideoId().isBlank()) continue;

            try {
                String url = video.getUrl() != null ? video.getUrl()
                        : "https://www.youtube.com/watch?v=" + video.getVideoId();

                if (bookmarkRepository.existsByUrlAndUserId(url, userId)) continue;

                Bookmark b = new Bookmark();
                b.setUser(user);
                b.setPlatform(PlatformType.YOUTUBE);
                b.setExternalId(video.getVideoId());
                b.setUrl(url);
                b.setTitle(video.getTitle() != null ? video.getTitle() : "Untitled Video");
                b.setDescription(buildDescription(video));
                b.setThumbnailUrl(video.getThumbnailUrl() != null ? video.getThumbnailUrl()
                        : "https://img.youtube.com/vi/" + video.getVideoId() + "/mqdefault.jpg");
                b.setContentType(ContentType.VIDEO);
                b.setCreatedAt(LocalDateTime.now());
                b.setUpdatedAt(LocalDateTime.now());
                bookmarkRepository.save(b);
                newCount++;

            } catch (Exception e) {
                log.warn("[YouTube] Skipped video {}: {}", video.getVideoId(), e.getMessage());
            }
        }

        long total = bookmarkRepository.countByUserIdAndPlatform(userId, PlatformType.YOUTUBE);

        log.info("[YouTube] user={} received={} new={} total={}",
                payload.getUserId(), payload.getVideos().size(), newCount, total);

        return YouTubeSyncResponse.builder()
                .status("success")
                .newVideos(newCount)
                .totalVideos((int) total)
                .lastSyncTime(LocalDateTime.now().toString())
                .message(String.format("YouTube: %d new video(s) from Watch Later", newCount))
                .build();
    }

    private String buildDescription(VideoItem video) {
        StringBuilder sb = new StringBuilder();
        if (video.getChannelName() != null && !video.getChannelName().isBlank()) {
            sb.append("Channel: ").append(video.getChannelName());
        }
        if (video.getDuration() != null && !video.getDuration().isBlank()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append("Duration: ").append(video.getDuration());
        }
        return sb.length() > 0 ? sb.toString() : "YouTube Watch Later video";
    }
}