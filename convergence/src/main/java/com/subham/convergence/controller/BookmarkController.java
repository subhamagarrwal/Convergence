package com.subham.convergence.controller;

import com.subham.convergence.dto.response.ApiResponse;
import com.subham.convergence.dto.response.BookmarkResponse;
import com.subham.convergence.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getAllBookmarks()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookmarkResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.getBookmarkById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        bookmarkService.deleteBookmark(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}