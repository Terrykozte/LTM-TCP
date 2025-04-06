package chatserver;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String username;
    private final ChatServer server;
    private final DatabaseManager dbManager;
    private String clientHostname;
    private String clientIpAddress;
    private int clientPort;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    
    public ClientHandler(Socket socket, ChatServer server, DatabaseManager dbManager) {
        this.socket = socket;
        this.server = server;
        this.dbManager = dbManager;
        this.clientHostname = socket.getInetAddress().getHostName();
        this.clientIpAddress = socket.getInetAddress().getHostAddress();
        this.clientPort = socket.getPort();
        
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Lỗi khi tạo client handler", ex);
        }
    }
    
    @Override
    public void run() {
        try {
            // Đọc username từ client
            username = reader.readLine();
            int serverPort = server.getServerPort();
            
            // Ghi log kết nối vào database với port
            dbManager.logConnection(username, clientIpAddress, true, serverPort);
            
            // Thông báo chi tiết về client mới kết nối
            server.logMessage("Client mới kết nối: " + username + " (" + clientIpAddress + ":" + 
                             clientPort + ") đến server port " + serverPort);
            
            // Thông báo cho các client khác
            server.broadcastMessage(username + " đã tham gia chat!", this);
            
            String message;
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("DISCONNECT:")) {
                    server.logMessage(username + " đã ngắt kết nối từ server port " + serverPort);
                    break;
                } else {
                    String fullMessage = username + ": " + message;
                    server.broadcastMessage(fullMessage, this);
                }
            }
        } catch (IOException ex) {
            if (username != null) {
                int serverPort = server.getServerPort();
                logger.log(Level.INFO, "Client ngắt kết nối đột ngột: " + username + " từ server port " + serverPort);
                server.logMessage("Client ngắt kết nối đột ngột: " + username + " từ server port " + serverPort);
            }
        } finally {
            close();
            server.removeClient(this);
            if (username != null) {
                // Ghi log ngắt kết nối vào database với port
                dbManager.logConnection(username, clientIpAddress, false, server.getServerPort());
                
                server.broadcastMessage(username + " đã rời chat!", this);
            }
        }
    }
    
    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }
    
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Lỗi khi đóng kết nối client", ex);
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getClientHostname() {
        return clientHostname;
    }
    
    public String getClientIpAddress() {
        return clientIpAddress;
    }
    
    public int getClientPort() {
        return clientPort;
    }
}