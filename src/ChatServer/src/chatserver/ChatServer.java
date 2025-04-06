package chatserver;

import java.io.*;
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final DatabaseManager dbManager;
    final ChatServerGUI gui;
    private boolean running = false;
    private int serverPort = 0;
    private int totalClients = 0;
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private final VigenereCipher cipher;

    public ChatServer(ChatServerGUI gui) {
        this.gui = gui;
        this.dbManager = new DatabaseManager();
        this.cipher = new VigenereCipher();
        setupLogger();
    }
    
    private void setupLogger() {
        try {
            Handler fileHandler = new FileHandler("chatserver.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(fileHandler);
            Logger.getLogger("").setLevel(Level.INFO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean start(int port) {
        try {
            // Kết nối đến cơ sở dữ liệu
            if (!dbManager.connect()) {
                gui.logMessage("Không thể kết nối đến cơ sở dữ liệu!");
                return false;
            }
            
            // Khởi tạo socket server
            serverSocket = new ServerSocket(port);
            serverPort = port;
            running = true;
            
            // Đọc thông tin từ cơ sở dữ liệu và hiển thị
            loadDatabaseInfo();
            
            // Reset client counters khi server khởi động
            synchronized (clients) {
                clients.clear();
                gui.updateClientCount(0);
            }
            totalClients = 0;
            gui.updateTotalClients(0);
            
            // Bắt đầu thread chấp nhận kết nối
            new Thread(new ConnectionAcceptor()).start();
            
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Không thể khởi động server trên port " + port, ex);
            gui.logMessage("Lỗi: " + ex.getMessage());
            return false;
        }
    }
    
    private void loadDatabaseInfo() {
        try {
            int totalMessages = dbManager.getMessagesCountByPort(serverPort);
            int totalUsers = dbManager.getUsersCountByPort(serverPort);
            
            gui.logMessage("Thông tin cơ sở dữ liệu cho port " + serverPort + ":");
            gui.logMessage("- Tổng số tin nhắn: " + totalMessages);
            gui.logMessage("- Tổng số người dùng đã kết nối: " + totalUsers);
            
            // Hiển thị 10 tin nhắn gần nhất cho port này
            ResultSet recentMessages = dbManager.getRecentMessages(10, serverPort);
            if (recentMessages != null) {
                boolean hasMessages = false;
                gui.logMessage("Tin nhắn gần đây trên port " + serverPort + ":");
                
                int count = 0;
                while (recentMessages.next() && count < 10) {
                    hasMessages = true;
                    String username = recentMessages.getString("username");
                    String message = recentMessages.getString("message");
                    String timestamp = recentMessages.getString("timestamp");
                    gui.logMessage("[" + timestamp + "] " + username + ": " + message);
                    count++;
                }
                
                if (!hasMessages) {
                    gui.logMessage("Chưa có tin nhắn nào trên port này.");
                }
                
                recentMessages.close();
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Không thể đọc thông tin từ cơ sở dữ liệu", ex);
        }
    }
    
    public void stop() {
        // Gửi thông báo đến tất cả client trước khi dừng
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage("SERVER_SHUTDOWN");
            }
            
            // Đợi một chút để đảm bảo thông điệp được gửi
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Đóng tất cả kết nối client
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
            gui.updateClientCount(0);
        }
        
        // Dừng server
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Đóng kết nối đến cơ sở dữ liệu
            dbManager.disconnect();
            logger.info("Server đã dừng trên port " + serverPort);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Lỗi khi đóng server", ex);
        }
    }
    
    public void deletePortData() {
        if (dbManager != null) {
            dbManager.deleteDataByPort(serverPort);
            gui.logMessage("Đã xóa toàn bộ dữ liệu trên port " + serverPort);
            
            // Cập nhật lại thông tin sau khi xóa
            loadDatabaseInfo();
        }
    }
    
    public void broadcastMessage(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
        
        // Phân tích tin nhắn
        String originalMessage = "";
        String encryptedContent = "";
        
        if (message.startsWith(sender.getUsername() + ": ")) {
            encryptedContent = message.substring((sender.getUsername() + ": ").length());
            String decryptedContent = cipher.decrypt(encryptedContent);
            originalMessage = sender.getUsername() + ": " + decryptedContent;
        } else {
            originalMessage = message;
        }
        
        // Lưu tin nhắn gốc vào database với port
        String hostname = sender.getClientHostname();
        String ipAddress = sender.getClientIpAddress();
        String username = sender.getUsername();
        dbManager.saveMessage(hostname, ipAddress, username, originalMessage, serverPort);
        
        // Kiểm tra nếu tin nhắn chứa nội dung mã hóa
        if (!encryptedContent.isEmpty()) {
            // Log tin nhắn trên server với thông tin so sánh
            gui.logCompareMessage(username, encryptedContent, cipher.decrypt(encryptedContent));
        } else {
            // Log tin nhắn trên server (thông báo hệ thống)
            gui.logMessage(message);
        }
    }
    
    public void removeClient(ClientHandler client) {
        synchronized (clients) {
            if (clients.remove(client)) {
                int currentCount = clients.size();
                gui.updateClientCount(currentCount);
                
                // Log thông tin về số client hiện tại
                gui.logMessage("Client đã ngắt kết nối. Số client hiện tại: " + currentCount);
            }
        }
    }
    
    public void addClient(ClientHandler client) {
        synchronized (clients) {
            clients.add(client);
            totalClients++;
            int currentCount = clients.size();
            
            gui.updateClientCount(currentCount);
            gui.updateTotalClients(totalClients);
            
            // Log thông tin về số client hiện tại
            gui.logMessage("Client mới kết nối. Số client hiện tại: " + currentCount + 
                          ", tổng số client đã kết nối: " + totalClients);
        }
    }
    
    public int getClientCount() {
        synchronized (clients) {
            return clients.size();
        }
    }
    
    public int getServerPort() {
        return serverPort;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }
    
    public VigenereCipher getCipher() {
        return cipher;
    }
    
    private class ConnectionAcceptor implements Runnable {
        @Override
        public void run() {
            gui.logMessage("Server đang lắng nghe kết nối trên port " + serverPort + "...");
            
            try {
                while (running) {
                    Socket socket = serverSocket.accept();
                    
                    ClientHandler client = new ClientHandler(socket, ChatServer.this, dbManager);
                    addClient(client);
                    
                    new Thread(client).start();
                }
            } catch (IOException ex) {
                if (running) {
                    logger.log(Level.SEVERE, "Lỗi khi chấp nhận kết nối", ex);
                    gui.logMessage("Lỗi khi chấp nhận kết nối: " + ex.getMessage());
                }
            }
        }
    }
    
    public void logMessage(String message) {
        gui.logMessage(message);
    }
}