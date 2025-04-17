package chatserver;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private ChatServer server;
    private DatabaseManager dbManager;
    private boolean connected = false;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private VigenereCipher cipher;
    private String clientIpAddress;
    private String clientHostname;
    
    public ClientHandler(Socket socket, ChatServer server, DatabaseManager dbManager) {
        this.socket = socket;
        this.server = server;
        this.dbManager = dbManager;
        this.cipher = server.getCipher();
        this.clientIpAddress = socket.getInetAddress().getHostAddress();
        try {
            this.clientHostname = socket.getInetAddress().getHostName();
        } catch (Exception e) {
            this.clientHostname = clientIpAddress;
        }
    }
    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            username = reader.readLine();
            
            // Kiểm tra nếu username đã đăng nhập ở nơi khác
            if (!server.handleUserLogin(username, this)) {
                // Đợi quyết định từ người dùng về việc tiếp tục đăng nhập hay không
                String response = reader.readLine();
                if (response.equals("FORCE_LOGIN")) {
                    // Tìm session cũ và ngắt kết nối
                    for (ClientHandler client : server.clients) {
                        if (client.getUsername() != null && client.getUsername().equals(username) && client != this) {
                            client.sendMessage("DISCONNECTED_BY_OTHER_SESSION");
                            client.disconnect();
                            break;
                        }
                    }
                } else {
                    // Người dùng chọn không tiếp tục đăng nhập
                    return;
                }
            }
            
            connected = true;
            server.logMessage("Client '" + username + "' đã kết nối từ " + clientIpAddress + " (" + clientHostname + ")");
            
            // Lưu thông tin kết nối vào cơ sở dữ liệu
            dbManager.logConnection(username, clientIpAddress, true, server.getServerPort());
            
            // Gửi thông báo cho tất cả người dùng biết người dùng mới đã kết nối
            String joinMessage = username + " đã tham gia chat!";
            server.broadcastMessage(joinMessage, this);
            
            // Gửi danh sách người dùng đang online cho client mới
            server.sendUserList(this);
            
            // Xử lý tin nhắn
            String message;
            while (connected && (message = reader.readLine()) != null) {
                if (message.equals("LOGOUT")) {
                    break;
                } else if (message.equals("REQUEST_CHAT_HISTORY")) {
                    // Xử lý yêu cầu lịch sử chat
                    server.sendChatHistory(username, this);
                } else if (message.equals("GET_ONLINE_USERS")) {
                    // Gửi danh sách người dùng đang online
                    server.sendUserList(this);
                } else if (message.startsWith("FILE_HEADER:")) {
                    // Xử lý gửi file
                    handleFileHeader(message);
                } else if (message.startsWith("REQUEST_FILE:")) {
                    // Xử lý yêu cầu file
                    String fileName = message.substring("REQUEST_FILE:".length());
                    sendFile(fileName);
                } else if (message.startsWith("READY_TO_RECEIVE:")) {
                    // Client sẵn sàng nhận file
                    String fileName = message.substring("READY_TO_RECEIVE:".length());
                    // Logic xử lý gửi file ở đây
                } else if (message.startsWith("FILE_RECEIVED:")) {
                    // Client đã nhận file thành công
                    String fileName = message.substring("FILE_RECEIVED:".length());
                    logger.info("Client " + username + " received file: " + fileName);
                } else if (message.equals("FORCE_LOGIN")) {
                    // Đã xử lý ở trên
                } else {
                    // Xử lý tin nhắn thông thường - đã được mã hóa từ client
                    server.broadcastMessage(username + ": " + message, this);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Lỗi kết nối cho " + username, e);
        } finally {
            disconnect();
        }
    }
    
    private void handleFileHeader(String header) {
        try {
            // FORMAT: FILE_HEADER:filename:filesize:filetype[:recipient]
            String[] parts = header.split(":", 5);
            String fileName = parts[1];
            long fileSize = Long.parseLong(parts[2]);
            String fileType = parts[3];
            String recipient = (parts.length > 4) ? parts[4] : null;
            
            // Kiểm tra kích thước file 
            long maxFileSize = 100 * 1024 * 1024; // 100MB
            if (fileSize > maxFileSize) {
                sendMessage("FILE_REJECTED:File too large (max 100MB)");
                return;
            }
            
            // Chấp nhận file
            sendMessage("FILE_ACCEPTED:" + fileName);
            
            // Tạo thư mục files nếu chưa tồn tại
            File filesDir = new File("files");
            if (!filesDir.exists()) {
                filesDir.mkdir();
            }
            
            // Xác định đường dẫn file
            String filePath = "files/" + fileName;
            File outputFile = new File(filePath);
            
            // Nếu file đã tồn tại, thêm timestamp để tránh trùng tên
            if (outputFile.exists()) {
                String baseName = fileName;
                String extension = "";
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    baseName = fileName.substring(0, lastDotIndex);
                    extension = fileName.substring(lastDotIndex);
                }
                fileName = baseName + "_" + System.currentTimeMillis() + extension;
                filePath = "files/" + fileName;
                outputFile = new File(filePath);
            }
            
            // Tạo stream để ghi file
            FileOutputStream fos = new FileOutputStream(outputFile);
            InputStream is = socket.getInputStream();
            
            // Nhận dữ liệu file
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalRead = 0;
            
            while (totalRead < fileSize && 
                   (bytesRead = is.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            
            fos.close();
            
            // Thông báo client đã nhận file thành công
            sendMessage("FILE_RECEIVED:" + fileName);
            
            // Lưu thông tin file vào database
            dbManager.saveFileInfo(username, fileName, fileType, fileSize, filePath, server.getServerPort());
            
            // Gửi header file cho client khác
            String fileHeader = "FILE_HEADER:" + fileName + ":" + fileSize + ":" + fileType + ":" + username;
            
            if (recipient != null && !recipient.isEmpty()) {
                // Gửi cho người nhận cụ thể
                server.sendFileHeaderToUser(fileHeader, recipient);
            } else {
                // Broadcast cho tất cả
                server.broadcastFileHeader(fileHeader, this);
            }
            
            server.logMessage("Client '" + username + "' đã gửi file: " + fileName + " (" + formatFileSize(fileSize) + ")");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi khi xử lý file header", e);
            sendMessage("FILE_REJECTED:Server error");
        }
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    private void sendFile(String fileName) {
        try {
            File file = new File("files/" + fileName);
            
            if (!file.exists()) {
                sendMessage("FILE_NOT_FOUND:" + fileName);
                return;
            }
            
            // Gửi thông báo file đã sẵn sàng
            String savePath = fileName; // Client sẽ quyết định đường dẫn lưu cuối cùng
            sendMessage("FILE_READY:" + fileName + ":" + savePath);
            
            // Gửi kích thước file
            sendMessage("FILE_SIZE:" + file.length());
            
            // Mở file stream
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = socket.getOutputStream();
            
            // Gửi dữ liệu file
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            
            os.flush();
            fis.close();
            
            server.logMessage("Đã gửi file " + fileName + " cho " + username);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi khi gửi file", e);
            sendMessage("FILE_SEND_ERROR:Server error");
        }
    }
    
    public void sendMessage(String message) {
        try {
            if (writer != null) {
                writer.println(message);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Lỗi khi gửi tin nhắn đến " + username, e);
        }
    }
    
    public void disconnect() {
        if (connected) {
            connected = false;
            
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Lỗi khi đóng kết nối", e);
            }
            
            // Lưu thông tin ngắt kết nối vào DB
            if (username != null) {
                dbManager.logConnection(username, clientIpAddress, false, server.getServerPort());
                
                // Phòng trường hợp đã đóng server
                if (server.isRunning()) {
                    // Thông báo cho tất cả client biết có người dùng rời đi
                    String leaveMessage = username + " đã rời chat!";
                    server.broadcastMessage(leaveMessage, this);
                    
                    // Thông báo cụ thể để client cập nhật danh sách người dùng
                    synchronized (server.clients) {
                        for (ClientHandler client : server.clients) {
                            if (client.isConnected() && !client.equals(this)) {
                                client.sendMessage("USER_DISCONNECTED:" + username);
                            }
                        }
                    }
                    
                    server.logMessage("Client '" + username + "' đã ngắt kết nối.");
                }
            }
            
            server.removeClient(this);
        }
    }
    
    public void close() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing client socket", e);
        }

        // Thông báo cho các client khác khi người dùng ngắt kết nối
        if (username != null) {
            for (ClientHandler client : server.clients) {
                if (client.isConnected() && client != this) {
                    client.sendMessage("USER_DISCONNECTED:" + username);
                }
            }
        }
    }
    public boolean isConnected() {
        return connected;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getClientIpAddress() {
        return clientIpAddress;
    }
    
    public String getClientHostname() {
        return clientHostname;
    }
}