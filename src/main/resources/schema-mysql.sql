-- MySQL-compatible schema for Cloud SQL (ardhana-db).
--
-- This is a translation of schema.sql (which is H2-only syntax and cannot
-- run on MySQL). The "cloud" profile (application-cloud.properties) points
-- Spring's schema init at this file with continue-on-error=true, so it runs
-- automatically on every startup: CREATE TABLE IF NOT EXISTS is a safe no-op
-- on repeat runs, and the CREATE INDEX statements (MySQL has no "IF NOT
-- EXISTS" for indexes) will log a harmless "already exists" error and be
-- skipped after the first successful run. You don't need to run this by
-- hand — it's here mainly for reference/manual recovery if needed.

CREATE TABLE IF NOT EXISTS cart_items (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    pillar_key VARCHAR(64) NOT NULL,
    pillar_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    quantity INT NOT NULL DEFAULT 1,
    added_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_cart_user_pillar UNIQUE (user_id, pillar_key)
);

CREATE INDEX idx_cart_user ON cart_items (user_id);

-- User profiles
CREATE TABLE IF NOT EXISTS profiles (
    id VARCHAR(36) PRIMARY KEY,
    supabase_uid VARCHAR(36) UNIQUE,
    full_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile VARCHAR(20),
    avatar_url VARCHAR(500),
    school_id VARCHAR(36),
    password_hash VARCHAR(255),
    country VARCHAR(2),
    bio VARCHAR(280),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User roles
CREATE TABLE IF NOT EXISTS user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);

CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    description TEXT,
    price_inr DECIMAL(10, 2),
    price_usd DECIMAL(10, 2),
    price_eur DECIMAL(10, 2),
    price_gbp DECIMAL(10, 2),
    sessions INT,
    level VARCHAR(20) DEFAULT 'BEGINNER',
    thumbnail_url VARCHAR(500),
    focuses TEXT,
    outcome TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schools (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    fee_amount DECIMAL(10, 2),
    fee_currency VARCHAR(3) DEFAULT 'INR',
    login_user_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_schools_name ON schools (name);

-- School platform fee payments — one row per Razorpay Checkout attempt.
CREATE TABLE IF NOT EXISTS school_payments (
    id VARCHAR(36) PRIMARY KEY,
    school_id VARCHAR(36) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    razorpay_order_id VARCHAR(64),
    razorpay_payment_id VARCHAR(64),
    razorpay_signature VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL
);

CREATE INDEX idx_school_payments_school ON school_payments (school_id);
CREATE INDEX idx_school_payments_razorpay_order ON school_payments (razorpay_order_id);

CREATE TABLE IF NOT EXISTS enrollments (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_enrollment UNIQUE (course_id, student_id)
);

CREATE INDEX idx_enrollments_course_id ON enrollments (course_id);
CREATE INDEX idx_enrollments_student_id ON enrollments (student_id);

CREATE TABLE IF NOT EXISTS lessons (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lessons_course_id ON lessons (course_id);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    razorpay_order_id VARCHAR(64),
    razorpay_payment_id VARCHAR(64),
    razorpay_signature VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_razorpay_order_id ON orders (razorpay_order_id);

CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    pillar_id VARCHAR(36),
    pillar_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    quantity INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    lesson_id CHAR(36) NOT NULL,
    student_id CHAR(36) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    pdf_url VARCHAR(500),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by CHAR(36),
    CONSTRAINT pk_lesson_progress PRIMARY KEY (id),
    CONSTRAINT uq_lesson_progress UNIQUE (lesson_id, student_id)
);

CREATE INDEX idx_lesson_progress_student_id ON lesson_progress (student_id);
CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress (lesson_id);

CREATE TABLE IF NOT EXISTS lesson_attachments (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    lesson_id CHAR(36) NOT NULL,
    student_id CHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_lesson_attachments PRIMARY KEY (id),
    CONSTRAINT uq_lesson_attachment UNIQUE (lesson_id, student_id)
);

-- Records successful MSG91 OTP verifications so AuthService.register() can
-- confirm a mobile was actually verified before creating the account.
CREATE TABLE IF NOT EXISTS phone_verifications (
    id VARCHAR(36) PRIMARY KEY,
    mobile VARCHAR(20) NOT NULL,
    verified_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_phone_verifications_mobile ON phone_verifications (mobile);

-- Profile photos, kept in their own table (like lesson_attachments) so the
-- BLOB isn't loaded every time a profile row is fetched.
CREATE TABLE IF NOT EXISTS profile_photos (
    user_id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(255),
    content_type VARCHAR(100) NOT NULL,
    file_data LONGBLOB NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- One-off scheduled classes: a course taught at a school by a teacher on a
-- specific date/time. Admin creates these; teachers see their own via /my.
CREATE TABLE IF NOT EXISTS class_schedules (
    id VARCHAR(36) PRIMARY KEY,
    school_id VARCHAR(36) NOT NULL,
    course_id VARCHAR(36) NOT NULL,
    teacher_id VARCHAR(36) NOT NULL,
    class_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    status_reason TEXT,
    status_updated_at TIMESTAMP NULL,
    assignment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    teacher_payout_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_class_schedules_school ON class_schedules (school_id);
CREATE INDEX idx_class_schedules_teacher ON class_schedules (teacher_id);
CREATE INDEX idx_class_schedules_date ON class_schedules (class_date);
CREATE INDEX idx_class_schedules_status ON class_schedules (status);
CREATE INDEX idx_class_schedules_assignment_status ON class_schedules (assignment_status);
CREATE INDEX idx_class_schedules_payout ON class_schedules (teacher_payout_id);

-- Teacher payments: per-teacher agreed rate + (teacher-entered-only) bank
-- details for RazorpayX payouts.
CREATE TABLE IF NOT EXISTS teacher_payout_profiles (
    user_id VARCHAR(36) PRIMARY KEY,
    rate_per_session DECIMAL(10, 2),
    account_holder_name VARCHAR(255),
    account_number VARCHAR(34),
    ifsc VARCHAR(11),
    upi_vpa VARCHAR(256),
    preferred_payout_method VARCHAR(20),
    razorpay_contact_id VARCHAR(64),
    razorpay_fund_account_id VARCHAR(64),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- One actual money transfer to a teacher, covering some number of their
-- completed-but-unpaid classes at the time it was made.
CREATE TABLE IF NOT EXISTS teacher_payouts (
    id VARCHAR(36) PRIMARY KEY,
    teacher_id VARCHAR(36) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    session_count INT NOT NULL DEFAULT 0,
    razorpay_payout_id VARCHAR(64),
    razorpay_fund_account_id VARCHAR(64),
    razorpay_contact_id VARCHAR(64),
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL
);

CREATE INDEX idx_teacher_payouts_teacher ON teacher_payouts (teacher_id);

-- Current Android app build (APK), single row per platform — kept out of
-- other tables like profile_photos/lesson_attachments so the BLOB is only
-- loaded when actually serving a download.
CREATE TABLE IF NOT EXISTS app_download_files (
    platform VARCHAR(20) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data LONGBLOB NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Name + mobile number captured from someone downloading the app.
CREATE TABLE IF NOT EXISTS app_download_leads (
    id VARCHAR(36) PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'android',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_download_leads_created_at ON app_download_leads (created_at);
