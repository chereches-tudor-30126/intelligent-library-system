package com.library.service.author.impl;

import com.library.dto.request.AuthorRequest;
import com.library.dto.response.AuthorResponse;
import com.library.entity.Author;
import com.library.repository.AuthorRepository;
import com.library.service.author.AuthorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<AuthorResponse> getAll(Pageable pageable) {
        return authorRepository.findAll(pageable)
                .map(AuthorResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorResponse getById(UUID id) {
        return AuthorResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthorResponse> getByNationality(String nationality) {
        return authorRepository.findAllByNationality(nationality)
                .stream()
                .map(AuthorResponse::from)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AuthorResponse create(AuthorRequest request) {
        Author author = Author.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .biography(request.biography())
                .birthYear(request.birthYear())
                .nationality(request.nationality())
                .build();

        Author saved = authorRepository.save(author);
        log.info("Author created: {} {} ({})", saved.getFirstName(), saved.getLastName(), saved.getId());
        return AuthorResponse.from(saved);
    }

    @Override
    @Transactional
    public AuthorResponse update(UUID id, AuthorRequest request) {
        Author author = findOrThrow(id);

        author.setFirstName(request.firstName());
        author.setLastName(request.lastName());
        author.setBiography(request.biography());
        author.setBirthYear(request.birthYear());
        author.setNationality(request.nationality());

        Author saved = authorRepository.save(author);
        log.info("Author updated: {} ({})", saved.getLastName(), id);
        return AuthorResponse.from(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Author author = findOrThrow(id);
        authorRepository.delete(author);
        log.warn("Author deleted: {} {} ({})", author.getFirstName(), author.getLastName(), id);
    }

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------

    private Author findOrThrow(UUID id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Author not found: " + id));
    }
}