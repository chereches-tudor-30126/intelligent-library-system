package com.library.dto.response;

import com.library.entity.Book;
import com.library.entity.BookType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private UUID id;
    private String isbn;
    private String title;
    private String description;
    private BookType bookType;
    private Short publishedYear;
    private String publisher;
    private String coverImageUrl;
    private Short totalCopies;
    private Short availableCopies;
    private String language;
    private Short pageCount;
    private BigDecimal averageRating;
    private Integer borrowCount;
    private Boolean isActive;
    private List<AuthorResponse> authors;  // safe — mapped manually, no lazy proxy
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static BookResponse from(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .description(book.getDescription())
                .bookType(book.getBookType())
                .publishedYear(book.getPublishedYear())
                .publisher(book.getPublisher())
                .coverImageUrl(book.getCoverImageUrl())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .language(book.getLanguage())
                .pageCount(book.getPageCount())
                .averageRating(book.getAverageRating())
                .borrowCount(book.getBorrowCount())
                .isActive(book.getIsActive())
                // Authors are loaded via JOIN in service — safe to map here
                .authors(book.getAuthors().stream()
                        .map(AuthorResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}