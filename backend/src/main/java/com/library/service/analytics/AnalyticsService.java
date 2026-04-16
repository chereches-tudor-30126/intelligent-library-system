package com.library.service.analytics;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface AnalyticsService {

    // Dashboard summary
    Map<String, Object> getDashboardSummary();

    // Books
    Map<String, Long>   getBookCountByType();
    List<Map<String, Object>> getTopBorrowedBooks(int limit);

    // Users
    Map<String, Long>   getUserCountByRole();
    long                getNewUsersCount(OffsetDateTime since);

    // Borrowings
    long                getActiveBorrowingsCount();
    long                getOverdueBorrowingsCount();
    long                getBorrowingsSince(OffsetDateTime since);
    Double              getOutstandingFinesTotal();

    // Trends
    List<Map<String, Object>> getBorrowingTrend(OffsetDateTime from, OffsetDateTime to, int limit);
}