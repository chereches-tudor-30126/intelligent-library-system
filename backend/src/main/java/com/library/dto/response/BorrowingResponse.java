package com.library.dto.response;

import com.library.entity.Borrowing;
import com.library.entity.BorrowingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingResponse {

    private UUID id;

    // Only safe scalar fields from User — no collections
    private UUID userId;
    private String userEmail;
    private String userFullName;

    // Only safe scalar fields from Book — no collections
    private UUID bookId;
    private String bookTitle;
    private String bookIsbn;

    private BorrowingStatus status;
    private OffsetDateTime borrowedAt;
    private OffsetDateTime dueDate;
    private OffsetDateTime returnedAt;
    private Short extendedCount;
    private BigDecimal fineAmount;
    private Boolean finePaid;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static BorrowingResponse from(Borrowing borrowing) {
        return BorrowingResponse.builder()
                .id(borrowing.getId())
                .userId(borrowing.getUser().getId())
                .userEmail(borrowing.getUser().getEmail())
                .userFullName(borrowing.getUser().getFirstName() + " "
                        + borrowing.getUser().getLastName())
                .bookId(borrowing.getBook().getId())
                .bookTitle(borrowing.getBook().getTitle())
                .bookIsbn(borrowing.getBook().getIsbn())
                .status(borrowing.getStatus())
                .borrowedAt(borrowing.getBorrowedAt())
                .dueDate(borrowing.getDueDate())
                .returnedAt(borrowing.getReturnedAt())
                .extendedCount(borrowing.getExtendedCount())
                .fineAmount(borrowing.getFineAmount())
                .finePaid(borrowing.getFinePaid())
                .notes(borrowing.getNotes())
                .createdAt(borrowing.getCreatedAt())
                .updatedAt(borrowing.getUpdatedAt())
                .build();
    }
}