package com.subham.convergence.controller;
import com.subham.convergence.dto.response.BookmarkResponse;
import com.subham.convergence.dto.response.ApiResponse;
import com.subham.convergence.service.BookmarkService;
import com.subham.convergence.dto.request.BookmarkRequest;
import com.subham.convergence.dto.response.AuthResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkService bookmarkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getAllBookmarks() {
        List<BookmarkResponse> bookmarks = bookmarkService.getAllBookmarks();
        return ResponseEntity.ok(
            ApiResponse.success(bookmarks, "Bookmarks retrieved successfully")
        );
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookmarkResponse>> getBookmarksById
    (@PathVariable UUID id){
        BookmarkResponse bookmark = bookmarkService.getBookmarkById(id);
        return ResponseEntity.ok(
            ApiResponse.success(bookmark, "Bookmark retrieved")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteBookmark(@PathVariable UUID id)
    {
        BookmarkResponse bookmark = bookmarkService.getBookmarkById(id);
        bookmarkService.deleteBookmark(id);
        return ResponseEntity.ok(
            ApiResponse.success(null, "Bookmark deleted successfully")
        );
    }
}
