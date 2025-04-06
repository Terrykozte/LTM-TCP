-- Tạo bảng messages để lưu tin nhắn
CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hostname TEXT,
    ip_address TEXT,
    username TEXT,
    message TEXT,
    original_message TEXT,  -- Thêm cột này để lưu tin nhắn gốc
    encrypted_message TEXT, -- Thêm cột này để lưu tin nhắn đã mã hóa
    server_port INTEGER,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng users để lưu thông tin người dùng
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE,
    password TEXT,           -- Thêm cột mật khẩu
    ip_address TEXT,
    last_login DATETIME DEFAULT CURRENT_TIMESTAMP,
    connection_count INTEGER DEFAULT 1
);

-- Tạo bảng connection_log để ghi log kết nối
CREATE TABLE IF NOT EXISTS connection_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT,
    ip_address TEXT,
    action TEXT, -- 'connect' hoặc 'disconnect'
    server_port INTEGER,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng files để lưu thông tin file được gửi
CREATE TABLE IF NOT EXISTS files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_username TEXT,
    file_name TEXT,
    file_type TEXT,
    file_size INTEGER,
    file_path TEXT,
    server_port INTEGER,
    sent_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Tạo chỉ mục để tìm kiếm nhanh
CREATE INDEX IF NOT EXISTS idx_messages_username ON messages(username);
CREATE INDEX IF NOT EXISTS idx_messages_port ON messages(server_port);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_connection_log_username ON connection_log(username);
CREATE INDEX IF NOT EXISTS idx_connection_log_port ON connection_log(server_port);
CREATE INDEX IF NOT EXISTS idx_files_sender ON files(sender_username);
CREATE INDEX IF NOT EXISTS idx_files_port ON files(server_port);