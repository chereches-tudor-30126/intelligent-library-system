package com.library.service.author;

import com.library.dto.request.AuthorRequest;
import com.library.dto.response.AuthorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AuthorService {

    Page<AuthorResponse> getAll(Pageable pageable);

    AuthorResponse getById(UUID id);

    List<AuthorResponse> getByNationality(String nationality);

    AuthorResponse create(AuthorRequest request);

    AuthorResponse update(UUID id, AuthorRequest request);

    void delete(UUID id);
}