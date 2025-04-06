package chatclient;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginRegisterManager {
    private static final Logger logger = Logger.getLogger(LoginRegisterManager.class.getName());
    private Connection connection;
    private String dbPath = "jdbc:sqlite:database/client_data.db";
    
    public LoginRegisterManager() {
        initDatabase();
    }
    
    private void initDatabase() {
        try {
            // Đảm bảo thư mục database tồn tại
            java.io.File dbDir = new java.io.File("database");
            if (!dbDir.exists()) {
                dbDir.mkdir();
            }
            
            // Kết nối đến database
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbPath);
            
            // Tạo bảng users nếu chưa tồn tại
            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
            
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTableSQL);
            }
            
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "Không thể kết nối đến database", e);
        }
    }
    
    public boolean login(String username, String password) {
        try {
            // Mã hóa mật khẩu
            String hashedPassword = hashPassword(password);
            
            // Kiểm tra thông tin đăng nhập
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next(); // Nếu có kết quả, đăng nhập thành công
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi đăng nhập", e);
        }
        return false;
    }
    
    public boolean register(String username, String password) {
        try {
            // Kiểm tra xem username đã tồn tại chưa
            String checkSql = "SELECT username FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        return false; // Username đã tồn tại
                    }
                }
            }
            
            // Mã hóa mật khẩu
            String hashedPassword = hashPassword(password);
            
            // Thêm người dùng mới
            String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi đăng ký", e);
        }
        return false;
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Lỗi khi mã hóa mật khẩu", e);
            return password; // Trả về password gốc nếu không thể mã hóa
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi đóng kết nối database", e);
        }
    }
}