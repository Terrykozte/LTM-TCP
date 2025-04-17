package chatserver;

import java.io.*;
import java.net.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

public class ChatServer {
    private ServerSocket serverSocket;
    final List<ClientHandler> clients = new ArrayList<>();
    private final DatabaseManager dbManager;
    final ChatServerGUI gui;
    private boolean running = false;
    private int serverPort = 0;
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private final VigenereCipher cipher;
    private Map<String, ClientHandler> userClientMap = new ConcurrentHashMap<>();

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
            if (!dbManager.connect()) {
                gui.logMessage("Không thể kết nối đến cơ sở dữ liệu!");
                return false;
            }
            
            serverSocket = new ServerSocket(port);
            serverPort = port;
            running = true;
            
            synchronized (clients) {
                clients.clear();
                gui.updateClientCount(0);
            }
            
            new Thread(new ConnectionAcceptor()).start();
            
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Không thể khởi động server trên port " + port, ex);
            gui.logMessage("Lỗi: " + ex.getMessage());
            return false;
        }
    }
    
    // Kiểm tra và xử lý khi một người dùng đăng nhập
    public boolean handleUserLogin(String username, ClientHandler newClient) {
        ClientHandler existingClient = userClientMap.get(username);

        if (existingClient != null && existingClient.isConnected()) {
            // Người dùng đã đăng nhập ở một client khác
            newClient.sendMessage("ACCOUNT_ALREADY_LOGGED_IN:" + username);
            logger.info("User " + username + " already logged in, notifying new client");
            return false;
        }

        // Đăng ký client mới cho username này
        userClientMap.put(username, newClient);

        // Gửi danh sách người dùng cho tất cả client
        sendUserList(newClient);

        // Broadcast thông báo người dùng mới tham gia
        broadcastMessage(username + " đã tham gia chat!", newClient);

        logger.info("User " + username + " logged in successfully");
        return true;
    }

    public void sendUserList(ClientHandler newClient) {
        List<String> userList = new ArrayList<>();
        
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.isConnected() && (newClient == null || !client.getUsername().equals(newClient.getUsername()))) {
                    userList.add(client.getUsername());
                }
            }
        }
        
        // Nếu newClient không null, gửi danh sách người dùng cho client mới
        if (newClient != null && !userList.isEmpty()) {
            StringBuilder userListMsg = new StringBuilder("USER_LIST:");
            for (String username : userList) {
                userListMsg.append(username).append(",");
            }
            newClient.sendMessage(userListMsg.toString());
        }
        
        // Thông báo cho các client khác biết có người dùng mới kết nối hoặc người dùng đã rời đi
        if (newClient != null) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client.isConnected() && !client.equals(newClient)) {
                        client.sendMessage("USER_CONNECTED:" + newClient.getUsername());
                    }
                }
            }
        } else {
            // Cập nhật danh sách người dùng khi có người rời đi
            StringBuilder userListMsg = new StringBuilder("USER_LIST:");
            for (String username : userList) {
                userListMsg.append(username).append(",");
            }
            
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client.isConnected()) {
                        client.sendMessage(userListMsg.toString());
                    }
                }
            }
        }
    }
    
    // Phương thức gửi lịch sử chat cho client
    public void sendChatHistory(String username, ClientHandler client) {
        try {
            // Lấy các tin nhắn gần đây từ cơ sở dữ liệu
            ResultSet history = dbManager.getMessagesWithEncryption(50, serverPort);
            
            client.sendMessage("CHAT_HISTORY_BEGIN");
            logger.info("Sending chat history to " + username);
            
            // Xử lý dữ liệu từ ResultSet
            while (history != null && history.next()) {
                String timestamp = history.getString("timestamp");
                String author = history.getString("username");
                String message;
                
                if (author.equals(username)) {
                    // Tin nhắn của chính người dùng này - sử dụng tin nhắn gốc
                    message = history.getString("original_message");
                } else {
                    // Tin nhắn từ người khác - gửi phiên bản đã mã hóa để client giải mã
                    message = history.getString("encrypted_message");
                }
                
                // Format: TIMESTAMP|AUTHOR|MESSAGE
                client.sendMessage(timestamp + "|" + author + "|" + message);
            }
            
            client.sendMessage("CHAT_HISTORY_END");
            
            if (history != null) {
                history.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error sending chat history", e);
            client.sendMessage("Error loading chat history");
        }
    }
    
    public void stop() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage("SERVER_SHUTDOWN");
            }
            
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            for (ClientHandler client : clients) {
                client.close();
            }
            clients.clear();
            gui.updateClientCount(0);
        }
        
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
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
        }
    }
    
    public void broadcastMessage(String message, ClientHandler sender) {
        // Kiểm tra xem đây có phải tin nhắn hệ thống (không cần mã hóa)
        boolean isSystemMessage = message.contains(" đã tham gia chat!") || 
                                 message.contains(" đã rời chat!") ||
                                 !message.contains(": ");
        
        if (isSystemMessage) {
            // Tin nhắn hệ thống - gửi nguyên trạng
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.sendMessage(message);
                }
            }
            
            gui.logMessage(message);
        } else {
            // Tin nhắn người dùng - gửi nguyên trạng vì đã được mã hóa từ client
            String senderName = message.substring(0, message.indexOf(": "));
            String encryptedContent = message.substring(message.indexOf(": ") + 2);
            
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client != sender) {
                        // Gửi tin nhắn đã mã hóa đến các client khác
                        client.sendMessage(message);
                    }
                }
            }
            
            // Lưu cả phiên bản gốc và mã hóa vào database
            String decryptedContent = cipher.decrypt(encryptedContent);
            String originalMessage = senderName + ": " + decryptedContent;
            
            String hostname = sender.getClientHostname();
            String ipAddress = sender.getClientIpAddress();
            String username = sender.getUsername();
            
            // Lưu với cả tin nhắn gốc và đã mã hóa
            dbManager.saveMessageWithEncryption(hostname, ipAddress, username, 
                                            originalMessage, message, serverPort);
            
            // Log để hiển thị
            gui.logCompareMessage(username, encryptedContent, decryptedContent);
        }
    }
    
    public void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
            
            if (client.getUsername() != null) {
                // Xóa khỏi map người dùng-client
                userClientMap.remove(client.getUsername());
                
                // Broadcast thông báo người dùng đã rời đi
                broadcastMessage(client.getUsername() + " đã rời chat!", null);
                
                // Cập nhật danh sách người dùng cho tất cả client còn lại
                sendUserList(null);
            }
            
            int currentCount = clients.size();
            gui.updateClientCount(currentCount);
            gui.logMessage("Client đã ngắt kết nối. Số client hiện tại: " + currentCount);
        }
    }  
    
    public void addClient(ClientHandler client) {
        synchronized (clients) {
            clients.add(client);
            int currentCount = clients.size();
            
            gui.updateClientCount(currentCount);
            gui.logMessage("Client mới kết nối. Số client hiện tại: " + currentCount);
        }
    }
    
    public void broadcastFileHeader(String header, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(header);
                }
            }
        }
    }
    
    public void sendFileHeaderToUser(String header, String recipient) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername().equals(recipient)) {
                    client.sendMessage(header);
                    logger.info("Sent file header to " + recipient);
                    break;
                }
            }
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