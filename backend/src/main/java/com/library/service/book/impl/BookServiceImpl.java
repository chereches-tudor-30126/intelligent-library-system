package com.library.service.book.impl;

import com.library.entity.Author;
import com.library.entity.Book;
import com.library.entity.BookType;
import com.library.repository.AuthorRepository;
import com.library.repository.BookRepository;
import com.library.service.audit.AuditLogService;
import com.library.service.book.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository   bookRepository;
    private final AuthorRepository authorRepository;
    private final AuditLogService  auditLogService;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Book getById(UUID id) {
        return bookRepository.findById(id)
                .filter(Book::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Book> getAll(Pageable pageable) {
        return bookRepository.findAllByIsActive(true, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Book> getAvailable(Pageable pageable) {
        return bookRepository.findAllAvailable(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Book> getByType(BookType type, Pageable pageable) {
        return bookRepository.findAllByBookTypeAndIsActive(type, true, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Book> search(String keyword, Pageable pageable) {
        return bookRepository.searchByKeyword(keyword, pageable);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public Book create(Book book, UUID... authorIds) {
        if (book.getIsbn() != null && bookRepository.existsByIsbn(book.getIsbn())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A book with ISBN " + book.getIsbn() + " already exists");
        }

        // Attach authors if provided
        if (authorIds != null) {
            Arrays.stream(authorIds).forEach(authorId -> {
                Author author = authorRepository.findById(authorId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Author not found: " + authorId));
                book.getAuthors().add(author);
            });
        }

        Book saved = bookRepository.save(book);
        auditLogService.logBookCreate(saved);
        log.info("Book created: '{}' ({})", saved.getTitle(), saved.getId());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public Book update(UUID id, Book updated) {
        Book existing = getById(id);
        Book snapshot = copySnapshot(existing);

        // ISBN uniqueness check — only if changed
        if (updated.getIsbn() != null
                && !updated.getIsbn().equals(existing.getIsbn())
                && bookRepository.existsByIsbn(updated.getIsbn())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ISBN already in use: " + updated.getIsbn());
        }

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setIsbn(updated.getIsbn());
        existing.setBookType(updated.getBookType());
        existing.setPublishedYear(updated.getPublishedYear());
        existing.setPublisher(updated.getPublisher());
        existing.setCoverImageUrl(updated.getCoverImageUrl());
        existing.setLanguage(updated.getLanguage());
        existing.setPageCount(updated.getPageCount());
        existing.setTotalCopies(updated.getTotalCopies());

        Book saved = bookRepository.save(existing);
        auditLogService.logBookUpdate(saved, snapshot);
        log.info("Book updated: '{}' ({})", saved.getTitle(), saved.getId());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Soft delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void delete(UUID id) {
        Book book = getById(id);
        book.setIsActive(false);
        bookRepository.save(book);
        auditLogService.logBookDelete(book);
        log.warn("Book soft-deleted: '{}' ({})", book.getTitle(), id);
    }

    // -------------------------------------------------------------------------
    // Author management
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void addAuthor(UUID bookId, UUID authorId) {
        Book book = getById(bookId);
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Author not found: " + authorId));
        book.getAuthors().add(author);
        bookRepository.save(book);
    }

    @Override
    @Transactional
    public void removeAuthor(UUID bookId, UUID authorId) {
        Book book = getById(bookId);
        book.getAuthors().removeIf(a -> a.getId().equals(authorId));
        bookRepository.save(book);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Book copySnapshot(Book book) {
        return Book.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .bookType(book.getBookType())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .build();
    }
}