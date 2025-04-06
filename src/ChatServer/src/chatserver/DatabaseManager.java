package chatserver;

import java.sql.*;
import java.util.logging.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DatabaseManager {
    private Connection connection;
    private String dbPath;
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    
    public DatabaseManager() {
        // Tạo đường dẫn đến thư mục database
        File dbDir = new File("1.database");
        if (!dbDir.exists()) {
            dbDir.mkdir();
        }
        
        this.dbPath = "jdbc:sqlite:1.database/chatapp.db";
    }
    
    public boolean connect() {
        try {
            // Kiểm tra driver SQLite
            Class.forName("org.sqlite.JDBC");
            
            // Kết nối đến database (tự động tạo nếu chưa tồn tại)
            connection = DriverManager.getConnection(dbPath);
            connection.setAutoCommit(true);
            
            // Tạo cấu trúc database nếu chưa tồn tại
            initializeDatabase();
            
            logger.info("Kết nối đến database thành công!");
            return true;
        } catch (ClassNotFoundException | SQLException ex) {
            logger.log(Level.SEVERE, "Không thể kết nối đến database", ex);
            return false;
        }
    }
    
    private void initializeDatabase() {
        try {
            // Đọc file SQL để khởi tạo
            File sqlFile = new File("1.database/chatapp.sql");
            
            if (sqlFile.exists()) {
                // Nếu file SQL tồn tại, đọc và thực thi nó
                try (Statement stmt = connection.createStatement();
                     java.util.Scanner scanner = new java.util.Scanner(sqlFile)) {
                    
                    StringBuilder sqlBuilder = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        // Bỏ qua các dòng comment
                        if (line.startsWith("--") || line.isEmpty()) continue;
                        
                        sqlBuilder.append(line);
                        if (line.endsWith(";")) {
                            stmt.execute(sqlBuilder.toString());
                            sqlBuilder.setLength(0);
                        }
                    }
                }
            } else {
                // Nếu file không tồn tại, tạo các bảng cần thiết
                createDefaultTables();
            }
            
            // Đảm bảo có cột server_port trong các bảng
            ensureServerPortColumn();
            
            logger.info("Database được khởi tạo thành công");
        } catch (SQLException | IOException ex) {
            logger.log(Level.SEVERE, "Lỗi khi khởi tạo database", ex);
        }
    }
    
    private void ensureServerPortColumn() {
        try (Statement stmt = connection.createStatement()) {
            // Kiểm tra xem cột server_port đã tồn tại trong bảng messages chưa
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(messages)");
            boolean hasServerPort = false;
            while (rs.next()) {
                if ("server_port".equals(rs.getString("name"))) {
                    hasServerPort = true;
                    break;
                }
            }
            
            // Thêm cột server_port nếu chưa có
            if (!hasServerPort) {
                stmt.execute("ALTER TABLE messages ADD COLUMN server_port INTEGER");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_port ON messages(server_port)");
            }
            
            // Kiểm tra xem cột server_port đã tồn tại trong bảng connection_log chưa
            rs = stmt.executeQuery("PRAGMA table_info(connection_log)");
            hasServerPort = false;
            while (rs.next()) {
                if ("server_port".equals(rs.getString("name"))) {
                    hasServerPort = true;
                    break;
                }
            }
            
            // Thêm cột server_port nếu chưa có
            if (!hasServerPort) {
                stmt.execute("ALTER TABLE connection_log ADD COLUMN server_port INTEGER");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_connection_log_port ON connection_log(server_port)");
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi kiểm tra/thêm cột server_port", ex);
        }
    }
    
    private void createDefaultTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Tạo bảng messages
            stmt.execute("CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "hostname TEXT,"
                    + "ip_address TEXT,"
                    + "username TEXT,"
                    + "message TEXT,"
                    + "server_port INTEGER,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            
            // Tạo bảng users
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT UNIQUE,"
                    + "ip_address TEXT,"
                    + "last_login DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "connection_count INTEGER DEFAULT 1"
                    + ");");
            
            // Tạo bảng connection_log
            stmt.execute("CREATE TABLE IF NOT EXISTS connection_log ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT,"
                    + "ip_address TEXT,"
                    + "action TEXT,"
                    + "server_port INTEGER,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            
            // Tạo các chỉ mục
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_username ON messages(username);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_port ON messages(server_port);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_connection_log_username ON connection_log(username);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_connection_log_port ON connection_log(server_port);");
        }
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Đã đóng kết nối database!");
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi đóng kết nối database", ex);
        }
    }
    
    public void saveMessage(String hostname, String ipAddress, String username, String message, int port) {
        try {
            String sql = "INSERT INTO messages (hostname, ip_address, username, message, server_port) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, hostname);
                pstmt.setString(2, ipAddress);
                pstmt.setString(3, username);
                pstmt.setString(4, message);
                pstmt.setInt(5, port);
                pstmt.executeUpdate();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi lưu tin nhắn", ex);
        }
    }
    
    public void logConnection(String username, String ipAddress, boolean isConnecting, int port) {
        try {
            // Ghi log kết nối/ngắt kết nối
            String action = isConnecting ? "connect" : "disconnect";
            String sql = "INSERT INTO connection_log (username, ip_address, action, server_port) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, ipAddress);
                pstmt.setString(3, action);
                pstmt.setInt(4, port);
                pstmt.executeUpdate();
            }
            
            // Cập nhật bảng users
            if (isConnecting) {
                // Kiểm tra người dùng đã tồn tại chưa
                try (PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT id, connection_count FROM users WHERE username = ?")) {
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next()) {
                        // Người dùng đã tồn tại, cập nhật thông tin
                        int userId = rs.getInt("id");
                        int connectionCount = rs.getInt("connection_count") + 1;
                        
                        try (PreparedStatement updateStmt = connection.prepareStatement(
                                "UPDATE users SET ip_address = ?, last_login = CURRENT_TIMESTAMP, connection_count = ? WHERE id = ?")) {
                            updateStmt.setString(1, ipAddress);
                            updateStmt.setInt(2, connectionCount);
                            updateStmt.setInt(3, userId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Người dùng mới, thêm vào database
                        try (PreparedStatement insertStmt = connection.prepareStatement(
                                "INSERT INTO users (username, ip_address) VALUES (?, ?)")) {
                            insertStmt.setString(1, username);
                            insertStmt.setString(2, ipAddress);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi ghi log kết nối", ex);
        }
    }
    
    public ResultSet getRecentMessages(int limit, int port) {
        try {
            String sql = "SELECT username, message, timestamp FROM messages WHERE server_port = ? ORDER BY timestamp DESC LIMIT ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, port);
            pstmt.setInt(2, limit);
            return pstmt.executeQuery();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi lấy tin nhắn gần đây", ex);
            return null;
        }
    }
    
    public ResultSet getActiveUsers(int port) {
        try {
            String sql = "SELECT DISTINCT u.username, u.last_login, u.connection_count FROM users u " +
                        "JOIN connection_log c ON u.username = c.username " +
                        "WHERE c.server_port = ? ORDER BY u.last_login DESC";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, port);
            return pstmt.executeQuery();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi lấy danh sách người dùng hoạt động", ex);
            return null;
        }
    }
    
    public int getMessagesCountByPort(int port) {
        try {
            String sql = "SELECT COUNT(*) as count FROM messages WHERE server_port = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, port);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi đếm tin nhắn theo port", ex);
            return 0;
        }
    }
    
    public int getUsersCountByPort(int port) {
        try {
            String sql = "SELECT COUNT(DISTINCT username) as count FROM connection_log WHERE server_port = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, port);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi đếm người dùng theo port", ex);
            return 0;
        }
    }
    
    public void deleteDataByPort(int port) {
        try {
            connection.setAutoCommit(false);
            
            // Xóa tin nhắn của port
            String sqlDeleteMessages = "DELETE FROM messages WHERE server_port = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteMessages)) {
                pstmt.setInt(1, port);
                int messagesDeleted = pstmt.executeUpdate();
                logger.info("Đã xóa " + messagesDeleted + " tin nhắn từ port " + port);
            }
            
            // Xóa log kết nối của port
            String sqlDeleteConnections = "DELETE FROM connection_log WHERE server_port = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteConnections)) {
                pstmt.setInt(1, port);
                int connectionsDeleted = pstmt.executeUpdate();
                logger.info("Đã xóa " + connectionsDeleted + " log kết nối từ port " + port);
            }
            
            connection.commit();
            connection.setAutoCommit(true);
            logger.info("Đã xóa toàn bộ dữ liệu từ port " + port);
        } catch (SQLException ex) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Lỗi khi rollback", e);
            }
            logger.log(Level.SEVERE, "Lỗi khi xóa dữ liệu theo port", ex);
        }
    }
    
    public void exportDataToTextFileByPort(String filePath, int port) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("=== LỊCH SỬ CHAT TRÊN PORT " + port + " ===");
            writer.println("Thời gian xuất: " + new java.util.Date());
            writer.println();

            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT timestamp, username, message FROM messages WHERE server_port = ? ORDER BY timestamp")) {
                stmt.setInt(1, port);
                ResultSet rs = stmt.executeQuery();
                
                boolean hasMessages = false;
                while (rs.next()) {
                    hasMessages = true;
                    String timestamp = rs.getString("timestamp");
                    String username = rs.getString("username");
                    String message = rs.getString("message");
                    
                    writer.println("[" + timestamp + "] " + username + ": " + message);
                }
                
                if (!hasMessages) {
                    writer.println("Không có tin nhắn nào trên port này.");
                }
            }
            
            writer.println();
            writer.println("=== THỐNG KÊ NGƯỜI DÙNG ===");
            writer.println();
            
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT DISTINCT u.username, u.last_login, u.connection_count FROM users u " +
                    "JOIN connection_log c ON u.username = c.username " +
                    "WHERE c.server_port = ? ORDER BY u.connection_count DESC")) {
                stmt.setInt(1, port);
                ResultSet rs = stmt.executeQuery();
                
                boolean hasUsers = false;
                while (rs.next()) {
                    hasUsers = true;
                    String username = rs.getString("username");
                    String lastLogin = rs.getString("last_login");
                    int connectionCount = rs.getInt("connection_count");
                    
                    writer.println(username + " - Đăng nhập gần nhất: " + lastLogin + " - Số lần kết nối: " + connectionCount);
                }
                
                if (!hasUsers) {
                    writer.println("Không có người dùng nào đã kết nối đến port này.");
                }
            }
            
            writer.println();
            writer.println("=== THỐNG KÊ KẾT NỐI ===");
            writer.println();
            
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) as connect_count FROM connection_log WHERE action = 'connect' AND server_port = ?")) {
                stmt.setInt(1, port);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int connectCount = rs.getInt("connect_count");
                    writer.println("Tổng số lần kết nối: " + connectCount);
                }
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) as disconnect_count FROM connection_log WHERE action = 'disconnect' AND server_port = ?")) {
                stmt.setInt(1, port);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int disconnectCount = rs.getInt("disconnect_count");
                    writer.println("Tổng số lần ngắt kết nối: " + disconnectCount);
                }
            }
            
            logger.info("Dữ liệu đã được xuất ra " + filePath);
        } catch (IOException | SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi xuất dữ liệu", e);
        }
    }

// Thêm vào DatabaseManager.java

public ResultSet searchMessages(String searchText, int port) {
    try {
        String sql = "SELECT username, message, timestamp FROM messages " +
                    "WHERE server_port = ? AND (message LIKE ? OR username LIKE ?) " +
                    "ORDER BY timestamp DESC";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, port);
        pstmt.setString(2, "%" + searchText + "%");
        pstmt.setString(3, "%" + searchText + "%");
        return pstmt.executeQuery();
    } catch (SQLException ex) {
        logger.log(Level.SEVERE, "Lỗi khi tìm kiếm tin nhắn", ex);
        return null;
    }
}

public void exportFilteredMessagesToFile(String filePath, String searchText, int port) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
        writer.println("=== TIN NHẮN TÌM KIẾM THEO TỪ KHÓA '" + searchText + "' TRÊN PORT " + port + " ===");
        writer.println("Thời gian xuất: " + new java.util.Date());
        writer.println("Từ khóa tìm kiếm: " + searchText);
        writer.println();

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT timestamp, username, message FROM messages " +
                "WHERE server_port = ? AND (message LIKE ? OR username LIKE ?) ORDER BY timestamp")) {
            stmt.setInt(1, port);
            stmt.setString(2, "%" + searchText + "%");
            stmt.setString(3, "%" + searchText + "%");
            ResultSet rs = stmt.executeQuery();
            
            boolean hasMessages = false;
            int count = 0;
            while (rs.next()) {
                hasMessages = true;
                count++;
                String timestamp = rs.getString("timestamp");
                String username = rs.getString("username");
                String message = rs.getString("message");
                
                writer.println("[" + timestamp + "] " + username + ": " + message);
            }
            
            if (!hasMessages) {
                writer.println("Không tìm thấy tin nhắn nào phù hợp với từ khóa '" + searchText + "'.");
            } else {
                writer.println();
                writer.println("Tổng số tin nhắn tìm thấy: " + count);
            }
        }
        
        logger.info("Dữ liệu tìm kiếm đã được xuất ra " + filePath);
    } catch (IOException | SQLException e) {
        logger.log(Level.SEVERE, "Lỗi khi xuất dữ liệu tìm kiếm", e);
    }
}
    
}