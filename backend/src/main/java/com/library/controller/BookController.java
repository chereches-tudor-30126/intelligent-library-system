package com.library.controller;

import com.library.entity.Book;
import com.library.entity.BookType;
import com.library.service.book.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // -------------------------------------------------------------------------
    // GET /api/v1/books — public, paginated, optional type filter
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<Page<Book>> getAll(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false)     BookType type) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Book> result = (type != null)
                ? bookService.getByType(type, pageable)
                : bookService.getAll(pageable);

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/books/available — public
    // -------------------------------------------------------------------------

    @GetMapping("/available")
    public ResponseEntity<Page<Book>> getAvailable(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookService.getAvailable(pageable));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/books/search?q=keyword — public, uses trigram index
    // -------------------------------------------------------------------------

    @GetMapping("/search")
    public ResponseEntity<Page<Book>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bookService.search(q, pageable));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/books/{id} — public
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<Book> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookService.getById(id));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/books — LIBRARIAN, ADMIN only
    // -------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Book> create(
            @Valid @RequestBody Book book,
            @RequestParam(required = false) UUID[] authorIds) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookService.create(book, authorIds));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/books/{id} — LIBRARIAN, ADMIN only
    // -------------------------------------------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Book> update(
            @PathVariable UUID id,
            @Valid @RequestBody Book book) {

        return ResponseEntity.ok(bookService.update(id, book));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/books/{id} — soft delete, ADMIN only
    // -------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/books/{bookId}/authors/{authorId} — LIBRARIAN, ADMIN
    // -------------------------------------------------------------------------

    @PostMapping("/{bookId}/authors/{authorId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Void> addAuthor(
            @PathVariable UUID bookId,
            @PathVariable UUID authorId) {

        bookService.addAuthor(bookId, authorId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/books/{bookId}/authors/{authorId} — LIBRARIAN, ADMIN
    // -------------------------------------------------------------------------

    @DeleteMapping("/{bookId}/authors/{authorId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Void> removeAuthor(
            @PathVariable UUID bookId,
            @PathVariable UUID authorId) {

        bookService.removeAuthor(bookId, authorId);
        return ResponseEntity.noContent().build();
    }
}