package com.library.repository;

import com.library.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuthorRepository extends JpaRepository<Author, UUID> {

    @Query("SELECT a FROM Author a WHERE " +
            "LOWER(a.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(a.lastName)  LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Author> searchByName(@Param("name") String name, Pageable pageable);

    List<Author> findAllByNationality(String nationality);

    boolean existsByFirstNameAndLastName(String firstName, String lastName);

    @Query("SELECT DISTINCT a FROM Author a JOIN a.books b WHERE b.isActive = true")
    List<Author> findAuthorsWithActiveBooks();
}