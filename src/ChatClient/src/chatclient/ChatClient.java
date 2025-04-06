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
    
    public ChatClient(ChatClientGUI gui) {
        this.gui = gui;
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
            
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Không thể kết nối đến server", ex);
            return false;
        }
    }
    
    public void sendMessage(String message) {
        if (writer != null && !message.trim().isEmpty() && connected) {
            writer.println(message);
            // Hiển thị tin nhắn của mình trên giao diện
            gui.displayMessage("Bạn: " + message);
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
                
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Lỗi khi ngắt kết nối", e);
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
                        gui.displayMessage("Server đã đóng kết nối. Bạn đã bị ngắt kết nối.");
                        gui.handleServerShutdown();
                        break;
                    }
                    // Chỉ hiển thị tin nhắn không bắt đầu bằng username:
                    else if (!message.startsWith(username + ":")) {
                        gui.displayMessage(message);
                    }
                }
            } catch (IOException ex) {
                if (connected) {
                    logger.log(Level.SEVERE, "Mất kết nối đến server", ex);
                    gui.displayMessage("Mất kết nối đến server!");
                    gui.handleServerShutdown();
                }
            } finally {
                connected = false;
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Lỗi khi đóng socket", e);
                }
            }
        }
    }
}