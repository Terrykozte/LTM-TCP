package chatserver;

import java.sql.*;
import java.util.logging.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection connection;
    private String dbPath;
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    private VigenereCipher cipher;
    
    public DatabaseManager() {
        File dbDir = new File("database");
        if (!dbDir.exists()) {
            dbDir.mkdir();
        }
        
        this.dbPath = "jdbc:sqlite:database/chatapp.db";
        this.cipher = new VigenereCipher();
    }
    
    public boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            
            connection = DriverManager.getConnection(dbPath);
            connection.setAutoCommit(true);
            
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
            File sqlFile = new File("database/chatapp.sql");
            
            if (sqlFile.exists()) {
                try (Statement stmt = connection.createStatement();
                     java.util.Scanner scanner = new java.util.Scanner(sqlFile)) {
                    
                    StringBuilder sqlBuilder = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        if (line.startsWith("--") || line.isEmpty()) continue;
                        
                        sqlBuilder.append(line);
                        if (line.endsWith(";")) {
                            stmt.execute(sqlBuilder.toString());
                            sqlBuilder.setLength(0);
                        }
                    }
                }
            } else {
                createDefaultTables();
            }
            
            ensureServerPortColumn();
            ensureEncryptionColumns();
            ensureChatHistoryTable();
            
            logger.info("Database được khởi tạo thành công");
        } catch (SQLException | IOException ex) {
            logger.log(Level.SEVERE, "Lỗi khi khởi tạo database", ex);
        }
    }
    
    private void ensureServerPortColumn() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(messages)");
            boolean hasServerPort = false;
            while (rs.next()) {
                if ("server_port".equals(rs.getString("name"))) {
                    hasServerPort = true;
                    break;
                }
            }
            
            if (!hasServerPort) {
                stmt.execute("ALTER TABLE messages ADD COLUMN server_port INTEGER");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_port ON messages(server_port)");
            }
            
            rs = stmt.executeQuery("PRAGMA table_info(connection_log)");
            hasServerPort = false;
            while (rs.next()) {
                if ("server_port".equals(rs.getString("name"))) {
                    hasServerPort = true;
                    break;
                }
            }
            
            if (!hasServerPort) {
                stmt.execute("ALTER TABLE connection_log ADD COLUMN server_port INTEGER");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_connection_log_port ON connection_log(server_port)");
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi kiểm tra/thêm cột server_port", ex);
        }
    }
    
    private void ensureEncryptionColumns() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(messages)");
            boolean hasOriginalMessage = false;
            boolean hasEncryptedMessage = false;
            
            while (rs.next()) {
                String colName = rs.getString("name");
                if ("original_message".equals(colName)) {
                    hasOriginalMessage = true;
                } else if ("encrypted_message".equals(colName)) {
                    hasEncryptedMessage = true;
                }
            }
            
            if (!hasOriginalMessage) {
                stmt.execute("ALTER TABLE messages ADD COLUMN original_message TEXT");
            }
            
            if (!hasEncryptedMessage) {
                stmt.execute("ALTER TABLE messages ADD COLUMN encrypted_message TEXT");
            }
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_original ON messages(original_message)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_encrypted ON messages(encrypted_message)");
            
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi kiểm tra/thêm cột mã hóa", ex);
        }
    }
    
    private void ensureChatHistoryTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS chat_history ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "server_port INTEGER,"
                    + "username TEXT,"
                    + "message TEXT,"
                    + "original_message TEXT,"
                    + "encrypted_message TEXT,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_port ON chat_history(server_port);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_username ON chat_history(username);");
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi tạo bảng chat_history", ex);
        }
    }
    
    private void createDefaultTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "hostname TEXT,"
                    + "ip_address TEXT,"
                    + "username TEXT,"
                    + "message TEXT,"
                    + "original_message TEXT,"
                    + "encrypted_message TEXT,"
                    + "server_port INTEGER,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT UNIQUE,"
                    + "password TEXT,"
                    + "ip_address TEXT,"
                    + "last_login DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "connection_count INTEGER DEFAULT 1"
                    + ");");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS connection_log ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT,"
                    + "ip_address TEXT,"
                    + "action TEXT,"
                    + "server_port INTEGER,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS files ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "sender_username TEXT,"
                    + "file_name TEXT,"
                    + "file_type TEXT,"
                    + "file_size INTEGER,"
                    + "file_path TEXT,"
                    + "server_port INTEGER,"
                    + "sent_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS chat_history ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "server_port INTEGER,"
                    + "username TEXT,"
                    + "message TEXT,"
                    + "original_message TEXT,"
                    + "encrypted_message TEXT,"
                    + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_username ON messages(username);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_port ON messages(server_port);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_original ON messages(original_message);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_encrypted ON messages(encrypted_message);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_connection_log_username ON connection_log(username);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_connection_log_port ON connection_log(server_port);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_sender ON files(sender_username);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_port ON files(server_port);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_port ON chat_history(server_port);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_username ON chat_history(username);");
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
            String originalContent = "";
            if (message.startsWith(username + ": ")) {
                originalContent = message.substring((username + ": ").length());
            } else {
                originalContent = message;
            }
            
            String encryptedContent = cipher.encrypt(originalContent);
            
            // Lưu vào messages
            String sql = "INSERT INTO messages (hostname, ip_address, username, message, original_message, encrypted_message, server_port) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, hostname);
                pstmt.setString(2, ipAddress);
                pstmt.setString(3, username);
                pstmt.setString(4, message);
                pstmt.setString(5, originalContent);
                pstmt.setString(6, encryptedContent);
                pstmt.setInt(7, port);
                pstmt.executeUpdate();
            }
            
            // Lưu vào chat_history
            sql = "INSERT INTO chat_history (server_port, username, message, original_message, encrypted_message) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, port);
                pstmt.setString(2, username);
                pstmt.setString(3, message);
                pstmt.setString(4, originalContent);
                pstmt.setString(5, encryptedContent);
                pstmt.executeUpdate();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi lưu tin nhắn", ex);
        }
    }
    
    public void saveMessageWithEncryption(String hostname, String ipAddress, String username, 
                                    String originalMessage, String encryptedMessage, int port) {
        try {
            String sql = "INSERT INTO messages (hostname, ip_address, username, message, original_message, encrypted_message, server_port) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, hostname);
                pstmt.setString(2, ipAddress);
                pstmt.setString(3, username);
                pstmt.setString(4, originalMessage);
                pstmt.setString(5, originalMessage.contains(": ") ? originalMessage.split(": ", 2)[1] : originalMessage);
                pstmt.setString(6, encryptedMessage.contains(": ") ? encryptedMessage.split(": ", 2)[1] : encryptedMessage);
                pstmt.setInt(7, port);
                pstmt.executeUpdate();
            }
            
            sql = "INSERT INTO chat_history (server_port, username, message, original_message, encrypted_message) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, port);
                pstmt.setString(2, username);
                pstmt.setString(3, originalMessage);
                pstmt.setString(4, originalMessage.contains(": ") ? originalMessage.split(": ", 2)[1] : originalMessage);
                pstmt.setString(5, encryptedMessage.contains(": ") ? encryptedMessage.split(": ", 2)[1] : encryptedMessage);
                pstmt.executeUpdate();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi lưu tin nhắn mã hóa", ex);
        }
    }
    
    public void logConnection(String username, String ipAddress, boolean isConnecting, int port) {
        try {
            String action = isConnecting ? "connect" : "disconnect";
            String sql = "INSERT INTO connection_log (username, ip_address, action, server_port) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, ipAddress);
                pstmt.setString(3, action);
                pstmt.setInt(4, port);
                pstmt.executeUpdate();
            }
            
            if (isConnecting) {
                try (PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT id, connection_count FROM users WHERE username = ?")) {
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next()) {
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
    
    // Lấy lịch sử chat
    public List<ChatMessage> getChatHistory(int port, int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT username, message, original_message, encrypted_message, timestamp FROM chat_history " +
                     "WHERE server_port = ? ORDER BY timestamp DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, port);
            pstmt.setInt(2, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String username = rs.getString("username");
                String message = rs.getString("message");
                String originalMessage = rs.getString("original_message");
                String encryptedMessage = rs.getString("encrypted_message");
                long timestamp = rs.getTimestamp("timestamp").getTime();
                
                messages.add(0, new ChatMessage(username, message, originalMessage, encryptedMessage, timestamp));
            }
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error retrieving chat history", e);
        }
        
        return messages;
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
    
    public ResultSet getMessagesWithEncryption(int limit, int port) {
        try {
            String sql = "SELECT username, message, original_message, encrypted_message, timestamp FROM messages WHERE server_port = ? ORDER BY timestamp DESC LIMIT ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, port);
            pstmt.setInt(2, limit);
            return pstmt.executeQuery();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi lấy tin nhắn mã hóa", ex);
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
            
            String sqlDeleteMessages = "DELETE FROM messages WHERE server_port = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteMessages)) {
                pstmt.setInt(1, port);
                int messagesDeleted = pstmt.executeUpdate();
                logger.info("Đã xóa " + messagesDeleted + " tin nhắn từ port " + port);
            }
            
            String sqlDeleteConnections = "DELETE FROM connection_log WHERE server_port = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteConnections)) {
                pstmt.setInt(1, port);
                int connectionsDeleted = pstmt.executeUpdate();
                logger.info("Đã xóa " + connectionsDeleted + " log kết nối từ port " + port);
            }
            
            String sqlDeleteFiles = "DELETE FROM files WHERE server_port = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteFiles)) {
                pstmt.setInt(1, port);
                int filesDeleted = pstmt.executeUpdate();
                logger.info("Đã xóa " + filesDeleted + " thông tin file từ port " + port);
            }
            
            String sqlDeleteHistory = "DELETE FROM chat_history WHERE server_port = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDeleteHistory)) {
                pstmt.setInt(1, port);
                int historyDeleted = pstmt.executeUpdate();
                logger.info("Đã xóa " + historyDeleted + " tin nhắn lịch sử từ port " + port);
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
                    "SELECT timestamp, username, message, original_message, encrypted_message FROM messages " +
                    "WHERE server_port = ? ORDER BY timestamp")) {
                stmt.setInt(1, port);
                ResultSet rs = stmt.executeQuery();
                
                boolean hasMessages = false;
                while (rs.next()) {
                    hasMessages = true;
                    String timestamp = rs.getString("timestamp");
                    String username = rs.getString("username");
                    String message = rs.getString("message");
                    String originalMsg = rs.getString("original_message");
                    String encryptedMsg = rs.getString("encrypted_message");
                    
                    writer.println("[" + timestamp + "] " + username + ": " + message);
                    
                    if (originalMsg != null && encryptedMsg != null && 
                        !originalMsg.isEmpty() && !encryptedMsg.isEmpty()) {
                        writer.println("   - Nội dung gốc: " + originalMsg);
                        writer.println("   - Mã hóa: " + encryptedMsg);
                        writer.println();
                    }
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
    
    public ResultSet searchMessagesWithEncryption(String searchText, int port) {
        try {
            String sql = "SELECT username, message, original_message, encrypted_message, timestamp FROM messages " +
                        "WHERE server_port = ? AND (message LIKE ? OR username LIKE ? OR original_message LIKE ?) " +
                        "ORDER BY timestamp DESC";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, port);
            pstmt.setString(2, "%" + searchText + "%");
            pstmt.setString(3, "%" + searchText + "%");
            pstmt.setString(4, "%" + searchText + "%");
            return pstmt.executeQuery();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi tìm kiếm tin nhắn mã hóa", ex);
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
                    "SELECT timestamp, username, message, original_message, encrypted_message FROM messages " +
                    "WHERE server_port = ? AND (message LIKE ? OR username LIKE ? OR original_message LIKE ?) ORDER BY timestamp")) {
                stmt.setInt(1, port);
                stmt.setString(2, "%" + searchText + "%");
                stmt.setString(3, "%" + searchText + "%");
                stmt.setString(4, "%" + searchText + "%");
                ResultSet rs = stmt.executeQuery();
                
                boolean hasMessages = false;
                int count = 0;
                while (rs.next()) {
                    hasMessages = true;
                    count++;
                    String timestamp = rs.getString("timestamp");
                    String username = rs.getString("username");
                    String message = rs.getString("message");
                    String originalMsg = rs.getString("original_message");
                    String encryptedMsg = rs.getString("encrypted_message");
                    
                    writer.println("[" + timestamp + "] " + username + ": " + message);
                    
                    if (originalMsg != null && encryptedMsg != null && 
                        !originalMsg.isEmpty() && !encryptedMsg.isEmpty()) {
                        writer.println("   - Nội dung gốc: " + originalMsg);
                        writer.println("   - Mã hóa: " + encryptedMsg);
                        writer.println();
                    }
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
    
    public void saveFileInfo(String username, String fileName, String fileType, long fileSize, String filePath, int port) {
        try {
            String sql = "INSERT INTO files (sender_username, file_name, file_type, file_size, file_path, server_port) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, fileName);
                pstmt.setString(3, fileType);
                pstmt.setLong(4, fileSize);
                pstmt.setString(5, filePath);
                pstmt.setInt(6, port);
                pstmt.executeUpdate();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi lưu thông tin file", ex);
        }
    }
    
    public ResultSet getFiles(int port) {
        try {
            String sql = "SELECT id, sender_username, file_name, file_type, file_size, file_path, sent_timestamp " +
                        "FROM files WHERE server_port = ? ORDER BY sent_timestamp DESC";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, port);
            return pstmt.executeQuery();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi lấy danh sách file", ex);
            return null;
        }
    }
    
    public boolean checkLogin(String username, String password) {
        try {
            String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi kiểm tra đăng nhập", ex);
            return false;
        }
    }
    
    public boolean registerUser(String username, String password) {
        try {
            String checkSql = "SELECT id FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    return false;
                }
            }
            
            String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Lỗi khi đăng ký người dùng", ex);
            return false;
        }
    }
}