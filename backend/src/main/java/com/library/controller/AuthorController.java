package com.library.controller;

import com.library.dto.request.AuthorRequest;
import com.library.dto.response.AuthorResponse;
import com.library.service.author.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    // GET /api/v1/authors — public
    @GetMapping
    public ResponseEntity<Page<AuthorResponse>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("lastName").ascending());
        return ResponseEntity.ok(authorService.getAll(pageable));
    }

    // GET /api/v1/authors/{id} — public
    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(authorService.getById(id));
    }

    // GET /api/v1/authors/nationality?q=Romanian — public
    @GetMapping("/nationality")
    public ResponseEntity<List<AuthorResponse>> getByNationality(@RequestParam String q) {
        return ResponseEntity.ok(authorService.getByNationality(q));
    }

    // POST /api/v1/authors — LIBRARIAN, ADMIN
    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<AuthorResponse> create(@Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authorService.create(request));
    }

    // PUT /api/v1/authors/{id} — LIBRARIAN, ADMIN
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<AuthorResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AuthorRequest request) {

        return ResponseEntity.ok(authorService.update(id, request));
    }

    // DELETE /api/v1/authors/{id} — ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}