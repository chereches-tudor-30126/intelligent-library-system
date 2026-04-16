package com.library.service.book;

import com.library.entity.Book;
import com.library.entity.BookType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookService {

    Book getById(UUID id);

    Page<Book> getAll(Pageable pageable);

    Page<Book> getAvailable(Pageable pageable);

    Page<Book> getByType(BookType type, Pageable pageable);

    Page<Book> search(String keyword, Pageable pageable);

    Book create(Book book, UUID... authorIds);

    Book update(UUID id, Book updated);

    void delete(UUID id);

    void addAuthor(UUID bookId, UUID authorId);

    void removeAuthor(UUID bookId, UUID authorId);
}