package com.library.service.analytics.impl;

import com.library.entity.BorrowingStatus;
import com.library.entity.Role;
import com.library.repository.BookRepository;
import com.library.repository.BorrowingRepository;
import com.library.repository.UserRepository;
import com.library.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookRepository     bookRepository;
    private final BorrowingRepository borrowingRepository;
    private final UserRepository     userRepository;

    // -------------------------------------------------------------------------
    // Dashboard summary — single call for the admin dashboard widget
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("totalBooks",         bookRepository.countByIsActive(true));
        summary.put("totalUsers",         userRepository.count());
        summary.put("activeBorrowings",   borrowingRepository.countByStatus(BorrowingStatus.ACTIVE));
        summary.put("overdueBorrowings",  borrowingRepository.countByStatus(BorrowingStatus.OVERDUE));
        summary.put("outstandingFines",   borrowingRepository.sumOutstandingFines());
        summary.put("newUsersThisMonth",  userRepository.countNewUsersSince(
                OffsetDateTime.now().minusMonths(1)));
        summary.put("borrowingsThisMonth", borrowingRepository.countBorrowingsSince(
                OffsetDateTime.now().minusMonths(1)));

        return summary;
    }

    // -------------------------------------------------------------------------
    // Books
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getBookCountByType() {
        List<Object[]> rows = bookRepository.countGroupedByBookType();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(row[0].toString(), (Long) row[1]);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopBorrowedBooks(int limit) {
        return bookRepository.findTop10ByIsActiveTrueOrderByBorrowCountDesc()
                .stream()
                .limit(limit)
                .map(book -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id",          book.getId());
                    entry.put("title",       book.getTitle());
                    entry.put("borrowCount", book.getBorrowCount());
                    entry.put("bookType",    book.getBookType());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Users
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getUserCountByRole() {
        Map<String, Long> result = new LinkedHashMap<>();
        Arrays.stream(Role.values()).forEach(role ->
                result.put(role.name(), userRepository.countByRole(role)));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long getNewUsersCount(OffsetDateTime since) {
        return userRepository.countNewUsersSince(since);
    }

    // -------------------------------------------------------------------------
    // Borrowings
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public long getActiveBorrowingsCount() {
        return borrowingRepository.countByStatus(BorrowingStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public long getOverdueBorrowingsCount() {
        return borrowingRepository.countByStatus(BorrowingStatus.OVERDUE);
    }

    @Override
    @Transactional(readOnly = true)
    public long getBorrowingsSince(OffsetDateTime since) {
        return borrowingRepository.countBorrowingsSince(since);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getOutstandingFinesTotal() {
        return borrowingRepository.sumOutstandingFines();
    }

    // -------------------------------------------------------------------------
    // Borrowing trend — top books in a time range
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBorrowingTrend(OffsetDateTime from,
                                                       OffsetDateTime to,
                                                       int limit) {
        List<Object[]> rows = borrowingRepository.findMostBorrowedBooksSince(
                from, PageRequest.of(0, limit));

        return rows.stream().map(row -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("bookId",  row[0]);
            entry.put("borrows", row[1]);
            return entry;
        }).collect(Collectors.toList());
    }
}