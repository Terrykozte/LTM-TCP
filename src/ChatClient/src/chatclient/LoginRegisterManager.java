package chatclient;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.logging.*;

/**
 * Quản lý thông tin đăng nhập và đăng ký người dùng.
 */
public class LoginRegisterManager {
    private static final String USER_FILE = "users.dat";
    private Map<String, String> users; // username -> hashed password
    private static final Logger logger = Logger.getLogger(LoginRegisterManager.class.getName());
    
    public LoginRegisterManager() {
        users = new HashMap<>();
        loadUsers();
    }
    
    /**
     * Đăng nhập người dùng.
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return true nếu thông tin đăng nhập chính xác
     */
    public boolean login(String username, String password) {
        if (!users.containsKey(username)) {
            logger.warning("Login attempt with non-existent username: " + username);
            return false;
        }
        
        String hashedPassword = hashPassword(password);
        boolean result = users.get(username).equals(hashedPassword);
        
        if (result) {
            logger.info("Login successful for user: " + username);
        } else {
            logger.warning("Login failed for user: " + username);
        }
        
        return result;
    }
    
    /**
     * Đăng ký người dùng mới.
     * 
     * @param username Tên đăng nhập mới
     * @param password Mật khẩu
     * @return true nếu đăng ký thành công
     */
    public boolean register(String username, String password) {
        if (users.containsKey(username)) {
            logger.warning("Registration attempt with existing username: " + username);
            return false;
        }
        
        String hashedPassword = hashPassword(password);
        users.put(username, hashedPassword);
        saveUsers();
        
        logger.info("User registered successfully: " + username);
        return true;
    }
    
    /**
     * Mã hóa mật khẩu.
     * 
     * @param password Mật khẩu cần mã hóa
     * @return Chuỗi mật khẩu đã được mã hóa
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error hashing password", e);
            return password; // Fallback (không an toàn)
        }
    }
    
    /**
     * Tải danh sách người dùng từ file.
     */
    private void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            logger.info("User file does not exist. Creating a new one.");
            // Tạo tài khoản mặc định nếu file không tồn tại
            users.put("admin", hashPassword("admin123!A"));
            saveUsers();
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            users = (Map<String, String>) ois.readObject();
            logger.info("Loaded " + users.size() + " users from file");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading users from file", e);
            // Nếu có lỗi khi đọc file, tạo file mới
            users.clear();
            users.put("admin", hashPassword("admin123!A"));
            saveUsers();
        }
    }
    
    /**
     * Lưu danh sách người dùng vào file.
     */
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
            logger.info("Saved " + users.size() + " users to file");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving users to file", e);
        }
    }
}

