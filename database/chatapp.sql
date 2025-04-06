-- Tạo bảng messages để lưu tin nhắn
CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hostname TEXT,
    ip_address TEXT,
    username TEXT,
    message TEXT,
    server_port INTEGER,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng users để lưu thông tin người dùng
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE,
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

-- Tạo chỉ mục để tìm kiếm nhanh
CREATE INDEX IF NOT EXISTS idx_messages_username ON messages(username);
CREATE INDEX IF NOT EXISTS idx_messages_port ON messages(server_port);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_connection_log_username ON connection_log(username);
CREATE INDEX IF NOT EXISTS idx_connection_log_port ON connection_log(server_port);