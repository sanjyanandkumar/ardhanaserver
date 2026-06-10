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
    price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    duration_hours INT,
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

-- Link students to schools
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS school_id VARCHAR(36);

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
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);

CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id),
    pillar_id VARCHAR(36),
    pillar_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1
);

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
