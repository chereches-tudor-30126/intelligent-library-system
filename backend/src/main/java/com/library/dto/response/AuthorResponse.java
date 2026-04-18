package com.library.dto.response;

import com.library.entity.Author;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String biography;
    private Short birthYear;
    private String nationality;
    private OffsetDateTime createdAt;

    public static AuthorResponse from(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .firstName(author.getFirstName())
                .lastName(author.getLastName())
                .biography(author.getBiography())
                .birthYear(author.getBirthYear())
                .nationality(author.getNationality())
                .createdAt(author.getCreatedAt())
                .build();
    }
}