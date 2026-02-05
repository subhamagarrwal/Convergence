package com.subham.convergence.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subham.convergence.dto.response.BookmarkResponse;
import com.subham.convergence.exception.BookmarkNotFoundException;
import com.subham.convergence.model.Bookmark;
import com.subham.convergence.repository.BookmarkRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public List<BookmarkResponse> getALLBookmarks()
    {
        List<Bookmark> bookmarks = bookmarkRepository.findAll();
        //convert to response dto
        List<BookmarkResponse> bookmarkResponses = bookmarks.stream()
                               .map(bookmark ->  BookmarkResponse.builder()
                               .id(bookmark.getId())
                               .title(bookmark.getTitle())
                               .url(bookmark.getUrl())
                               .platform(bookmark.getPlatform())
                               .contentType(bookmark.getContentType()) 
                               .build())
                            .toList();
        return bookmarkResponses;
    }
    @Transactional
    public BookmarkResponse getBookmarkById(UUID id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                            .orElseThrow(() -> new BookmarkNotFoundException("bookmark doesnt exist"));

        return BookmarkResponse.builder()
                               .id(bookmark.getId())
                               .title(bookmark.getTitle())
                               .url(bookmark.getUrl())
                               .platform(bookmark.getPlatform())
                               .contentType(bookmark.getContentType()) 
                               .build();
    }
    @Transactional
    public void deleteBookmark(UUID id) {
        Bookmark bookmark = bookmarkRepository.findById(id)
                            .orElseThrow(() -> new BookmarkNotFoundException("bookmark doesnt exist"));
        bookmarkRepository.delete(bookmark);
    }

    @Transactional
    public List<BookmarkResponse> getBookmarksByPlatform(String platform) {
        List<Bookmark> bookmarks = bookmarkRepository.findByPlatform(platform);
        //convert to response dto
        List<BookmarkResponse> bookmarkResponses = bookmarks.stream()
                               .map(bookmark ->  BookmarkResponse.builder()
                               .id(bookmark.getId())
                               .title(bookmark.getTitle())
                               .url(bookmark.getUrl())
                               .platform(bookmark.getPlatform())
                               .contentType(bookmark.getContentType()) 
                               .build())
                            .toList();
        return bookmarkResponses;
    }    

}
