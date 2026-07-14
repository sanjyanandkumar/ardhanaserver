-- Cart items
CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    pillar_key VARCHAR(64) NOT NULL,
    pillar_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    quantity INT NOT NULL DEFAULT 1,
    added_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_cart_user_pillar UNIQUE (user_id, pillar_key)
);

CREATE INDEX IF NOT EXISTS idx_cart_user ON cart_items (user_id);
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'INR';

-- User profiles
CREATE TABLE IF NOT EXISTS profiles (
    id VARCHAR(36) PRIMARY KEY,
    supabase_uid VARCHAR(36) UNIQUE,
    full_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile VARCHAR(20),
    avatar_url VARCHAR(500),
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

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles (user_id);

CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_schools_name ON schools (name);

-- School platform fees + login: admin sets an owed amount, the school pays
-- it via their own login account (created on demand — see login_user_id).
ALTER TABLE schools ADD COLUMN IF NOT EXISTS fee_amount DECIMAL(10, 2);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS fee_currency VARCHAR(3) DEFAULT 'INR';
ALTER TABLE schools ADD COLUMN IF NOT EXISTS login_user_id VARCHAR(36);

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
    confirmed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_school_payments_school ON school_payments (school_id);
CREATE INDEX IF NOT EXISTS idx_school_payments_razorpay_order ON school_payments (razorpay_order_id);

-- One nullable price per supported currency — a course only needs a price
-- set for the currencies it's actually sold in.
ALTER TABLE courses ADD COLUMN IF NOT EXISTS price_inr DECIMAL(10, 2);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS price_usd DECIMAL(10, 2);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS price_eur DECIMAL(10, 2);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS price_gbp DECIMAL(10, 2);

-- Course length is now measured in sessions, not hours.
ALTER TABLE courses ADD COLUMN IF NOT EXISTS sessions INT;

-- Legacy single price/currency/duration_hours columns, fully superseded by
-- the columns above — dropped outright.
ALTER TABLE courses DROP COLUMN IF EXISTS price;
ALTER TABLE courses DROP COLUMN IF EXISTS currency;
ALTER TABLE courses DROP COLUMN IF EXISTS duration_hours;

-- Primary display name, shown above "title" (title becomes a subtitle line
-- shown below it). Nullable — existing courses won't have one until edited.
ALTER TABLE courses ADD COLUMN IF NOT EXISTS name VARCHAR(255);

-- Link students to schools
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS school_id VARCHAR(36);

-- App-managed authentication (replaces Supabase Auth)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- Captured at signup, used to pick which currency's price to show
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS country VARCHAR(2);

-- Short (~2-line) bio, editable from the profile page (teachers only, in the
-- UI). Powers the public "Meet Our Teachers" landing-page section.
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS bio VARCHAR(280);

CREATE TABLE IF NOT EXISTS enrollments (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_enrollment UNIQUE (course_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_enrollments_course_id ON enrollments (course_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_student_id ON enrollments (student_id);

CREATE TABLE IF NOT EXISTS lessons (
    id VARCHAR(36) PRIMARY KEY,
    course_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lessons_course_id ON lessons (course_id);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2) NOT NULL,
    razorpay_order_id VARCHAR(64),
    razorpay_payment_id VARCHAR(64),
    razorpay_signature VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(64);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(64);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS razorpay_signature VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'INR';
CREATE INDEX IF NOT EXISTS idx_orders_razorpay_order_id ON orders (razorpay_order_id);

CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id),
    pillar_id VARCHAR(36),
    pillar_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    quantity INT NOT NULL DEFAULT 1
);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'INR';

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id UUID DEFAULT RANDOM_UUID() NOT NULL,
    lesson_id UUID NOT NULL,
    student_id UUID NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    CONSTRAINT pk_lesson_progress PRIMARY KEY (id),
    CONSTRAINT uq_lesson_progress UNIQUE (lesson_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_lesson_progress_student_id ON lesson_progress (student_id);
CREATE INDEX IF NOT EXISTS idx_lesson_progress_lesson_id ON lesson_progress (lesson_id);

ALTER TABLE lesson_progress ADD COLUMN IF NOT EXISTS pdf_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS lesson_attachments (
    id UUID DEFAULT RANDOM_UUID() NOT NULL,
    lesson_id UUID NOT NULL,
    student_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_data BLOB NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_phone_verifications_mobile ON phone_verifications (mobile);

-- Profile photos, kept in their own table (like lesson_attachments) so the
-- BLOB isn't loaded every time a profile row is fetched.
CREATE TABLE IF NOT EXISTS profile_photos (
    user_id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(255),
    content_type VARCHAR(100) NOT NULL,
    file_data BLOB NOT NULL,
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_class_schedules_school ON class_schedules (school_id);
CREATE INDEX IF NOT EXISTS idx_class_schedules_teacher ON class_schedules (teacher_id);
CREATE INDEX IF NOT EXISTS idx_class_schedules_date ON class_schedules (class_date);

-- Session completion tracking: the assigned teacher (or an admin) marks a
-- class COMPLETED or NOT_COMPLETED (with a reason) once its scheduled end
-- time has passed. Defaults to SCHEDULED for every existing row.
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED';
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS status_reason TEXT;
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS status_updated_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_class_schedules_status ON class_schedules (status);

-- Whether the assigned teacher has accepted a class or flagged themselves
-- unavailable, set BEFORE the class happens (separate from `status` above,
-- which is the after-the-fact outcome). Defaults to PENDING for every
-- existing row.
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS assignment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
CREATE INDEX IF NOT EXISTS idx_class_schedules_assignment_status ON class_schedules (assignment_status);

-- Teacher payments: per-teacher agreed rate + (teacher-entered-only) bank
-- details for RazorpayX payouts.
CREATE TABLE IF NOT EXISTS teacher_payout_profiles (
    user_id VARCHAR(36) PRIMARY KEY,
    rate_per_session DECIMAL(10, 2),
    account_holder_name VARCHAR(255),
    account_number VARCHAR(34),
    ifsc VARCHAR(11),
    razorpay_contact_id VARCHAR(64),
    razorpay_fund_account_id VARCHAR(64),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- UPI as an alternative payout method alongside bank account details.
ALTER TABLE teacher_payout_profiles ADD COLUMN IF NOT EXISTS upi_vpa VARCHAR(256);
ALTER TABLE teacher_payout_profiles ADD COLUMN IF NOT EXISTS preferred_payout_method VARCHAR(20);

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
    processed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_teacher_payouts_teacher ON teacher_payouts (teacher_id);

-- Current Android app build (APK), single row per platform — kept out of
-- other tables like profile_photos/lesson_attachments so the BLOB is only
-- loaded when actually serving a download.
CREATE TABLE IF NOT EXISTS app_download_files (
    platform VARCHAR(20) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    file_data BLOB NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_app_download_leads_created_at ON app_download_leads (created_at);

-- Shared, admin-wide alerts (e.g. a teacher marked a class unavailable) —
-- not per-recipient; every admin sees the same list and read/unread state.
CREATE TABLE IF NOT EXISTS admin_notifications (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    message TEXT NOT NULL,
    related_class_schedule_id VARCHAR(36),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_notifications_created_at ON admin_notifications (created_at);
CREATE INDEX IF NOT EXISTS idx_admin_notifications_read ON admin_notifications (is_read);

-- FCM device tokens registered by admin app installs, so PushNotificationService
-- knows where to send alerts. Keyed by token so re-registering upserts.
CREATE TABLE IF NOT EXISTS admin_push_tokens (
    token VARCHAR(255) PRIMARY KEY,
    admin_user_id VARCHAR(36) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_push_tokens_user ON admin_push_tokens (admin_user_id);

-- Links a completed class session to the payout that paid it. NULL = unpaid.
ALTER TABLE class_schedules ADD COLUMN IF NOT EXISTS teacher_payout_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_class_schedules_payout ON class_schedules (teacher_payout_id);
