package com.library.controller;

import com.library.dto.response.BookResponse;
import com.library.entity.BookType;
import com.library.service.book.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

import com.library.entity.Book;
import jakarta.validation.Valid;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<Page<BookResponse>> getAll(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false)     BookType type) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        var books = (type != null)
                ? bookService.getByType(type, pageable)
                : bookService.getAll(pageable);

        return ResponseEntity.ok(toPage(books, pageable));
    }

    @GetMapping("/available")
    public ResponseEntity<Page<BookResponse>> getAvailable(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(toPage(bookService.getAvailable(pageable), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BookResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(toPage(bookService.search(q, pageable), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(BookResponse.from(bookService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> create(
            @Valid @RequestBody Book book,
            @RequestParam(required = false) UUID[] authorIds) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookResponse.from(bookService.create(book, authorIds)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody Book book) {

        return ResponseEntity.ok(BookResponse.from(bookService.update(id, book)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/authors/{authorId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Void> addAuthor(
            @PathVariable UUID bookId,
            @PathVariable UUID authorId) {
        bookService.addAuthor(bookId, authorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{bookId}/authors/{authorId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<Void> removeAuthor(
            @PathVariable UUID bookId,
            @PathVariable UUID authorId) {
        bookService.removeAuthor(bookId, authorId);
        return ResponseEntity.noContent().build();
    }

    private Page<BookResponse> toPage(Page<Book> books, Pageable pageable) {
        return new PageImpl<>(
                books.getContent().stream()
                        .map(BookResponse::from)
                        .collect(Collectors.toList()),
                pageable,
                books.getTotalElements()
        );
    }
}