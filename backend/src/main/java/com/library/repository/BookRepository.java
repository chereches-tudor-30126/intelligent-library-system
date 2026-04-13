package com.library.repository;

import com.library.entity.Book;
import com.library.entity.BookType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    Page<Book> findAllByIsActive(Boolean isActive, Pageable pageable);

    Page<Book> findAllByBookType(BookType bookType, Pageable pageable);

    Page<Book> findAllByBookTypeAndIsActive(BookType bookType, Boolean isActive, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.availableCopies > 0 AND b.isActive = true")
    Page<Book> findAllAvailable(Pageable pageable);


    @Query("SELECT b FROM Book b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')) AND b.isActive = true")
    Page<Book> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.authors a WHERE " +
            "LOWER(a.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(a.lastName)  LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Book> findByAuthorName(@Param("name") String name, Pageable pageable);

    @Modifying
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies - 1 WHERE b.id = :id AND b.availableCopies > 0")
    int decrementAvailableCopies(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies + 1 WHERE b.id = :id")
    void incrementAvailableCopies(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Book b SET b.borrowCount = b.borrowCount + 1 WHERE b.id = :id")
    void incrementBorrowCount(@Param("id") UUID id);


    List<Book> findTop10ByIsActiveTrueOrderByBorrowCountDesc();

    long countByBookType(BookType bookType);

    long countByIsActive(Boolean isActive);

    @Query("SELECT b.bookType, COUNT(b) FROM Book b GROUP BY b.bookType")
    List<Object[]> countGroupedByBookType();
}