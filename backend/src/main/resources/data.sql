-- =============================================================================
-- data.sql — Test data for Intelligent Library Management System
-- Runs AFTER Hibernate creates tables (defer-datasource-initialization: true)
-- Passwords are BCrypt of "Parola1234!"
-- =============================================================================

-- Safe cleanup — order matters due to FK constraints
DELETE FROM book_authors;
DELETE FROM borrowings;
DELETE FROM notifications;
DELETE FROM audit_logs;
DELETE FROM refresh_tokens;
DELETE FROM books;
DELETE FROM authors;
DELETE FROM users;

-- =============================================================================
-- USERS
-- =============================================================================

INSERT INTO users (id, email, username, password_hash, first_name, last_name,
                   role, is_active, is_email_verified, failed_login_attempts)
VALUES
    ('u1000000-0000-0000-0000-000000000001',
     'admin@library.ro', 'admin',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj0AegTNsua2',
     'Admin', 'System', 'ADMIN', true, true, 0),

    ('u1000000-0000-0000-0000-000000000002',
     'librarian@library.ro', 'librarian',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj0AegTNsua2',
     'Maria', 'Ionescu', 'LIBRARIAN', true, true, 0),

    ('u1000000-0000-0000-0000-000000000003',
     'student@library.ro', 'student',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj0AegTNsua2',
     'Ion', 'Popescu', 'STUDENT', true, true, 0);


-- =============================================================================
-- AUTHORS
-- =============================================================================

INSERT INTO authors (id, first_name, last_name, biography, birth_year, nationality)
VALUES
    ('a1000000-0000-0000-0000-000000000001',
     'Robert', 'Martin',
     'Software engineer known as Uncle Bob. Pioneer of Clean Code and SOLID principles.',
     1952, 'American'),

    ('a1000000-0000-0000-0000-000000000002',
     'Martin', 'Fowler',
     'Chief Scientist at ThoughtWorks. Expert in software design and refactoring.',
     1963, 'British'),

    ('a1000000-0000-0000-0000-000000000003',
     'Joshua', 'Bloch',
     'Former Google engineer. Author of Effective Java, the definitive Java guide.',
     1961, 'American'),

    ('a1000000-0000-0000-0000-000000000004',
     'Donald', 'Knuth',
     'Professor emeritus at Stanford. Creator of TeX and The Art of Computer Programming.',
     1938, 'American'),

    ('a1000000-0000-0000-0000-000000000005',
     'Andrew', 'Hunt',
     'Co-author of The Pragmatic Programmer and co-founder of the Agile Manifesto.',
     1964, 'American');


-- =============================================================================
-- BOOKS
-- =============================================================================

INSERT INTO books (id, isbn, title, description, book_type, published_year,
                   publisher, language, page_count, total_copies, available_copies,
                   borrow_count, is_active)
VALUES
    ('b1000000-0000-0000-0000-000000000001',
     '9780132350884', 'Clean Code',
     'A handbook of agile software craftsmanship.',
     'TECHNOLOGY', 2008, 'Prentice Hall', 'English', 431, 3, 3, 12, true),

    ('b1000000-0000-0000-0000-000000000002',
     '9780201485677', 'Refactoring',
     'Improving the Design of Existing Code.',
     'TECHNOLOGY', 1999, 'Addison-Wesley', 'English', 448, 3, 3, 8, true),

    ('b1000000-0000-0000-0000-000000000003',
     '9780321125217', 'Effective Java',
     'Best practices for the Java platform. Essential reading for every Java developer.',
     'TECHNOLOGY', 2018, 'Addison-Wesley', 'English', 412, 4, 4, 15, true),

    ('b1000000-0000-0000-0000-000000000004',
     '9780201896831', 'The Art of Computer Programming Vol. 1',
     'Fundamental Algorithms. The foundational work on computer science.',
     'SCIENCE', 1997, 'Addison-Wesley', 'English', 672, 1, 1, 3, true),

    ('b1000000-0000-0000-0000-000000000005',
     '9780135957059', 'The Pragmatic Programmer',
     'Your journey to mastery. Practical advice for better software development.',
     'TECHNOLOGY', 2019, 'Addison-Wesley', 'English', 352, 3, 3, 20, true),

    ('b1000000-0000-0000-0000-000000000006',
     '9780596007126', 'Head First Design Patterns',
     'A brain-friendly guide to design patterns using Java.',
     'TECHNOLOGY', 2004, 'O''Reilly Media', 'English', 694, 2, 2, 7, true),

    ('b1000000-0000-0000-0000-000000000007',
     '9780321127426', 'Patterns of Enterprise Application Architecture',
     'Martin Fowler''s catalog of patterns for enterprise software.',
     'TECHNOLOGY', 2002, 'Addison-Wesley', 'English', 533, 2, 2, 5, true),

    ('b1000000-0000-0000-0000-000000000008',
     '9780062316097', 'Sapiens',
     'A Brief History of Humankind.',
     'HISTORY', 2011, 'Harper', 'English', 443, 3, 3, 18, true),

    ('b1000000-0000-0000-0000-000000000009',
     '9780140449136', 'The Iliad',
     'Ancient Greek epic poem attributed to Homer.',
     'FICTION', 762, 'Penguin Classics', 'English', 704, 2, 2, 4, true),

    ('b1000000-0000-0000-0000-000000000010',
     '9780743273565', 'The Great Gatsby',
     'A story of the fabulously wealthy Jay Gatsby.',
     'FICTION', 1925, 'Scribner', 'English', 180, 5, 5, 9, true);


-- =============================================================================
-- BOOK_AUTHORS
-- =============================================================================

INSERT INTO book_authors (book_id, author_id)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001'),
    ('b1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002'),
    ('b1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000003'),
    ('b1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000004'),
    ('b1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000005'),
    ('b1000000-0000-0000-0000-000000000006', 'a1000000-0000-0000-0000-000000000002'),
    ('b1000000-0000-0000-0000-000000000007', 'a1000000-0000-0000-0000-000000000002');