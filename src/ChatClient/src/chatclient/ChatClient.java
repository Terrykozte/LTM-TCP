package chatclient;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ChatClient {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String username;
    private ChatClientGUI gui;
    private boolean connected = false;
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
    private VigenereCipher cipher;
    
    public ChatClient(ChatClientGUI gui) {
        this.gui = gui;
        this.cipher = new VigenereCipher();
    }
    
    public boolean connect(String serverAddress, int port, String username) {
        try {
            this.username = username;
            socket = new Socket(serverAddress, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Gửi thông tin đăng nhập
            writer.println(username);
            connected = true;
            
            // Bắt đầu thread để đọc tin nhắn từ server
            new Thread(new MessageReader()).start();
            
            logger.info("Connected to server " + serverAddress + " on port " + port + " as " + username);
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Cannot connect to server", ex);
            return false;
        }
    }
    
    public void sendMessage(String message) {
        if (writer != null && !message.trim().isEmpty() && connected) {
            try {
                // Mã hóa tin nhắn trước khi gửi
                String encryptedMessage = cipher.encrypt(message);
                writer.println(encryptedMessage);
                
                // Hiển thị tin nhắn gốc của mình trên giao diện
                gui.displaySentMessage(message);
                logger.info("Message sent: " + message);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error sending message", ex);
                gui.displaySystemMessage("Lỗi khi gửi tin nhắn: " + ex.getMessage());
            }
        }
    }
    
    public void sendFile(File file) {
        if (!connected || file == null || !file.exists()) {
            gui.displaySystemMessage("Không thể gửi file. Vui lòng kiểm tra kết nối và file.");
            return;
        }
        
        try {
            // Get file info
            String fileName = file.getName();
            long fileSize = file.length();
            String formattedSize = FileTransferHandler.formatFileSize(fileSize);
            
            // Display file sending status
            gui.displaySystemMessage("Đang gửi file: " + fileName + " (" + formattedSize + ")...");
            
            // Send file command
            String fileCommand = "FILE:" + fileName + ":" + fileSize;
            writer.println(fileCommand);
            
            // In a real application, you would implement the file transfer logic here
            // This is a placeholder
            gui.displaySystemMessage("Chức năng gửi file đang được phát triển...");
            
            logger.info("File sending initiated: " + fileName + " (" + formattedSize + ")");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error sending file", ex);
            gui.displaySystemMessage("Lỗi khi gửi file: " + ex.getMessage());
        }
    }
    
    public void disconnect() {
        if (socket != null && socket.isConnected() && connected) {
            try {
                connected = false;
                int serverPort = socket.getPort();
                String serverAddress = socket.getInetAddress().getHostAddress();
                
                if (writer != null) {
                    writer.println("DISCONNECT:" + username + ":" + serverAddress + ":" + serverPort);
                    writer.flush();
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                socket.close();
                
                logger.info("Disconnected from server");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error when disconnecting", e);
            } finally {
                socket = null;
            }
        }
    }
    
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    private class MessageReader implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    // Xử lý thông báo server shutdown
                    if (message.equals("SERVER_SHUTDOWN")) {
                        gui.displaySystemMessage("Server đã đóng kết nối. Bạn đã bị ngắt kết nối.");
                        gui.handleServerShutdown();
                        break;
                    }
                    // Xử lý tin nhắn file
                    else if (message.startsWith("FILE:")) {
                        handleFileMessage(message);
                    }
                    // Chỉ hiển thị tin nhắn không bắt đầu bằng username:
                    else if (!message.startsWith(username + ":")) {
                        // Kiểm tra xem có phải là thông báo hệ thống không
                        if (message.contains(" đã tham gia chat!") || message.contains(" đã rời chat!")) {
                            gui.displaySystemMessage(message);
                        } else {
                            // Phân tách username và nội dung mã hóa
                            int colonIndex = message.indexOf(": ");
                            if (colonIndex > 0) {
                                String sender = message.substring(0, colonIndex);
                                String encryptedContent = message.substring(colonIndex + 2);
                                
                                // Giải mã tin nhắn
                                String decryptedContent = cipher.decrypt(encryptedContent);
                                
                                // Hiển thị tin nhắn đã giải mã
                                gui.displayReceivedMessage(sender + ": " + decryptedContent);
                                logger.info("Message received from " + sender);
                            } else {
                                gui.displaySystemMessage(message);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                if (connected) {
                    logger.log(Level.SEVERE, "Lost connection to server", ex);
                    gui.displaySystemMessage("Mất kết nối đến server!");
                    gui.handleServerShutdown();
                }
            } finally {
                connected = false;
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing socket", e);
                }
            }
        }
    }
    
    private void handleFileMessage(String message) {
        // Parse file message: FILE:username:filename:filesize
        String[] parts = message.split(":", 4);
        if (parts.length < 4) {
            gui.displaySystemMessage("Thông báo file không hợp lệ: " + message);
            return;
        }
        
        String sender = parts[1];
        String fileName = parts[2];
        long fileSize = Long.parseLong(parts[3]);
        String formattedSize = FileTransferHandler.formatFileSize(fileSize);
        
        // Display file message
        gui.displaySystemMessage(sender + " muốn gửi file: " + fileName + " (" + formattedSize + ")");
        
        // In a real application, you would implement file receiving logic here
    }
    
    public String getUsername() {
        return username;
    }
}