package chatclient;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.awt.Desktop;

public class ChatClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private boolean connected = false;
    private ChatClientGUI gui;
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
    private VigenereCipher cipher;
    private String serverAddress;
    private int serverPort;
    private Queue<FileTransferRequest> fileTransferQueue = new ConcurrentLinkedQueue<>();
    private boolean fileTransferInProgress = false;
    
    // Thêm bộ theo dõi tin nhắn trùng lặp
    private Set<String> recentConnectedUsers = new HashSet<>();
    private static final long CONNECTION_MESSAGE_TIMEOUT = 5000; // 5 giây timeout
    
    // Hằng số cho quá trình truyền file
    private static final int BUFFER_SIZE = 8192;
    private static final int PROGRESS_UPDATE_INTERVAL = 5; // cập nhật tiến độ mỗi 5%
    
    public class FileTransferRequest {
        private final File file;
        private final String recipient;
        
        public FileTransferRequest(File file, String recipient) {
            this.file = file;
            this.recipient = recipient;
        }
        
        public File getFile() {
            return file;
        }
        
        public String getRecipient() {
            return recipient;
        }
    }
    
    public ChatClient(ChatClientGUI gui) {
        this.gui = gui;
        this.cipher = new VigenereCipher();
        setupLogger();
    }
    
    private void setupLogger() {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdir();
            }
            
            Handler fileHandler = new FileHandler("logs/chatclient.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(fileHandler);
            Logger.getLogger("").setLevel(Level.INFO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean connect(String serverAddress, int port, String username) {
        try {
            this.serverAddress = serverAddress;
            this.serverPort = port;
            this.username = username;

            socket = new Socket(serverAddress, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Gửi username đến server
            writer.println(username);

            // Bắt đầu thread đọc tin nhắn từ server
            new Thread(new MessageReader()).start();

            // Bắt đầu thread xử lý file
            new Thread(new FileTransferProcessor()).start();

            connected = true;

            // Yêu cầu danh sách người dùng ngay sau khi kết nối
            requestOnlineUsers();

            logger.info("Connected to server: " + serverAddress + ":" + port + " as " + username);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not connect to server", e);
            return false;
        }
    }

    public boolean isConnected() {
        return connected;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void sendMessage(String message) {
        if (connected) {
            try {
                // Gửi tin nhắn thô đến server, không mã hóa
                writer.println(message);
                logger.info("Message sent: " + message);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error sending message", e);
                gui.displaySystemMessage("Lỗi khi gửi tin nhắn: " + e.getMessage());
            }
        } else {
            gui.displaySystemMessage("Bạn chưa kết nối đến server!");
        }
    }
    
    public void requestOnlineUsers() {
        if (connected) {
            writer.println("GET_ONLINE_USERS");
            logger.info("Requested online users list");
        }
    }
    
    public void sendFile(File file) {
        // Add file to queue, with null recipient for broadcast
        fileTransferQueue.add(new FileTransferRequest(file, null));
        logger.info("Added file to transfer queue: " + file.getName());
        // Hiển thị thông báo đang chuẩn bị gửi file
        gui.displaySystemMessage("Đang chuẩn bị gửi file: " + file.getName());
    }
    
    public void sendFileToUser(File file, String recipient) {
        fileTransferQueue.add(new FileTransferRequest(file, recipient));
        logger.info("Added file to transfer queue for " + recipient + ": " + file.getName());
        // Hiển thị thông báo đang chuẩn bị gửi file
        gui.displaySystemMessage("Đang chuẩn bị gửi file đến " + recipient + ": " + file.getName());
    }
    
    public String getFileTypeFromExtension(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        
        if (extension.matches("jpg|jpeg|png|gif|bmp|tiff|webp")) {
            return "image";
        } else if (extension.matches("mp3|wav|ogg|flac|aac|m4a|wma")) {
            return "audio";
        } else if (extension.matches("mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp")) {
            return "video";
        } else if (extension.matches("pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|odt|ods|odp")) {
            return "document";
        } else {
            return "other";
        }
    }
    
    private String getFileSizeFormat(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    private class FileTransferProcessor implements Runnable {
        @Override
        public void run() {
            while (connected) {
                try {
                    // If a file transfer is in progress, wait
                    if (fileTransferInProgress) {
                        Thread.sleep(500);
                        continue;
                    }
                    
                    // Get next file from queue
                    FileTransferRequest request = fileTransferQueue.poll();
                    if (request == null) {
                        Thread.sleep(500);
                        continue;
                    }
                    
                    fileTransferInProgress = true;
                    File file = request.getFile();
                    String recipient = request.getRecipient();
                    
                    try {
                        // Get file information
                        String fileName = file.getName();
                        long fileSize = file.length();
                        String fileType = getFileTypeFromExtension(fileName);
                        String fileSizeDisplay = getFileSizeFormat(fileSize);
                        
                        // Notify server about the file
                        String header = "FILE_HEADER:" + fileName + ":" + fileSize + ":" + fileType + ":" + fileSizeDisplay;
                        if (recipient != null) {
                            header += ":" + recipient;  // Add recipient if specified
                        }
                        writer.println(header);
                        logger.info("Sent file header: " + header);
                        
                        // Wait for server acknowledgment
                        String response = reader.readLine();
                        if (response.startsWith("FILE_ACCEPTED:")) {
                            // Server is ready to receive the file
                            sendFileData(file);
                        } else {
                            logger.warning("File transfer rejected: " + response);
                            gui.displaySystemMessage("Yêu cầu gửi file bị từ chối: " + response);
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error initiating file transfer", e);
                        gui.displaySystemMessage("Lỗi khi gửi file: " + e.getMessage());
                    } finally {
                        fileTransferInProgress = false;
                    }
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "File transfer processor interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unexpected error in file transfer processor", e);
                }
            }
        }
        
        private void sendFileData(File file) throws IOException {
            FileInputStream fis = null;
            OutputStream os = null;
            
            try {
                fis = new FileInputStream(file);
                os = socket.getOutputStream();
                
                // Send the file bytes
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytesSent = 0;
                long fileSize = file.length();
                int lastProgressPercentage = 0;
                
                // Display file sending notification in chat
                gui.displaySystemMessage("Đang gửi file: " + file.getName() + " (0%)");
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    totalBytesSent += bytesRead;
                    
                    // Update progress
                    int progressPercentage = (int) ((totalBytesSent * 100) / fileSize);
                    if (progressPercentage >= lastProgressPercentage + PROGRESS_UPDATE_INTERVAL) {
                        lastProgressPercentage = progressPercentage;
                        gui.updateFileProgress(file.getName(), progressPercentage);
                    }
                }
                
                os.flush();
                
                // Wait for confirmation from server
                String confirmation = reader.readLine();
                if (confirmation.startsWith("FILE_RECEIVED:")) {
                    gui.updateFileProgress(file.getName(), 100);
                    logger.info("File sent successfully: " + file.getName());
                    gui.displaySystemMessage("File đã được gửi thành công: " + file.getName());
                    
                    // Extract file details from confirmation message
                    String[] parts = confirmation.split(":", 3);
                    if (parts.length >= 3) {
                        String fileName = parts[1];
                        String fileDetails = parts[2];
                        // Trigger display of file in chat
                        String fileType = getFileTypeFromExtension(fileName);
                        gui.displayFileMessage(username, fileName, fileType, fileDetails);
                    }
                } else {
                    logger.warning("Unexpected response after file transfer: " + confirmation);
                    gui.displaySystemMessage("Có lỗi xảy ra khi gửi file: " + confirmation);
                }
            } finally {
                if (fis != null) fis.close();
                // Don't close os as it would close the socket
            }
        }
    }
    
    public void requestFile(String fileName, String savePath) {
        if (connected) {
            try {
                writer.println("REQUEST_FILE:" + fileName + ":" + savePath);
                logger.info("Requested file: " + fileName + " to be saved at: " + savePath);
                gui.displaySystemMessage("Đã yêu cầu tải file: " + fileName);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error requesting file", e);
                gui.displaySystemMessage("Lỗi khi yêu cầu file!");
            }
        } else {
            gui.displaySystemMessage("Bạn chưa kết nối đến server!");
        }
    }
    
    private class MessageReader implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.equals("SERVER_SHUTDOWN")) {
                        gui.displaySystemMessage("Server đã đóng kết nối. Bạn đã bị ngắt kết nối.");
                        gui.handleServerShutdown();
                        break;
                    } else if (message.equals("DISCONNECTED_BY_OTHER_SESSION")) {
                        gui.displaySystemMessage("Tài khoản của bạn đã đăng nhập ở nơi khác. Bạn đã bị ngắt kết nối.");
                        gui.handleServerShutdown();
                        break;
                    } else if (message.startsWith("ACCOUNT_ALREADY_LOGGED_IN:")) {
                        String username = message.substring("ACCOUNT_ALREADY_LOGGED_IN:".length());
                        gui.handleExistingSession(username);
                    } else if (message.startsWith("CHAT_HISTORY_BEGIN")) {
                        receiveAndDisplayChatHistory();
                    } else if (message.startsWith("FILE_HEADER:")) {
                        handleFileHeader(message);
                    } else if (message.startsWith("FILE_READY:")) {
                        String[] parts = message.split(":", 3);
                        if (parts.length >= 3) {
                            String fileName = parts[1];
                            String savePath = parts[2];
                            receiveFile(fileName, savePath);
                        }
                    } else if (message.startsWith("USER_LIST:")) {
                        // Xử lý danh sách người dùng từ server
                        processUserList(message.substring("USER_LIST:".length()));
                    } else if (message.startsWith("USER_CONNECTED:")) {
                        // Người dùng mới kết nối
                        String newUser = message.substring("USER_CONNECTED:".length());
                        if (!newUser.equals(username)) {
                            // Kiểm tra xem người dùng này đã hiển thị gần đây chưa
                            String userKey = newUser + "_connected";
                            if (!recentConnectedUsers.contains(userKey)) {
                                gui.displaySystemMessage(newUser + " đã tham gia chat!");
                                gui.updateUserList(newUser, true);
                                
                                // Thêm vào tập hợp người dùng gần đây và lên lịch xóa
                                recentConnectedUsers.add(userKey);
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        recentConnectedUsers.remove(userKey);
                                    }
                                }, CONNECTION_MESSAGE_TIMEOUT);
                            }
                        }
                    } else if (message.startsWith("USER_DISCONNECTED:")) {
                        // Người dùng ngắt kết nối
                        String leftUser = message.substring("USER_DISCONNECTED:".length());
                        if (!leftUser.equals(username)) {
                            String userKey = leftUser + "_disconnected";
                            if (!recentConnectedUsers.contains(userKey)) {
                                gui.displaySystemMessage(leftUser + " đã rời chat!");
                                gui.updateUserList(leftUser, false);
                                
                                // Thêm vào tập hợp người dùng gần đây và lên lịch xóa
                                recentConnectedUsers.add(userKey);
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        recentConnectedUsers.remove(userKey);
                                    }
                                }, CONNECTION_MESSAGE_TIMEOUT);
                            }
                        }
                    } else if (message.startsWith("FILE_MESSAGE:")) {
                        // Process file message from another user
                        String[] parts = message.split(":", 5);
                        if (parts.length >= 5) {
                            String sender = parts[1];
                            String fileName = parts[2];
                            String fileType = parts[3];
                            String fileDetails = parts[4];
                            
                            // Display the file notification in the chat window
                            gui.displayFileMessage(sender, fileName, fileType, fileDetails);
                        }
                    } else if (message.startsWith(username + ":")) {
                        // Tin nhắn từ chính mình - server echo lại
                        // Bỏ qua vì đã hiển thị khi gửi
                        continue;
                    } else if (message.contains(" đã tham gia chat!")) {
                        // Bỏ qua thông báo này vì đã được xử lý ở USER_CONNECTED
                        // Tránh hiển thị trùng lặp
                        continue;
                    } else if (message.contains(" đã rời chat!")) {
                        // Bỏ qua thông báo này vì đã được xử lý ở USER_DISCONNECTED
                        // Tránh hiển thị trùng lặp
                        continue;
                    } else if (message.contains(": ")) {
                        // Tin nhắn từ người khác - cần tách người gửi và nội dung đúng cách
                        gui.displayReceivedMessage(message);
                    } else {
                        // Tin nhắn hệ thống khác
                        gui.displaySystemMessage(message);
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

        private void receiveAndDisplayChatHistory() throws IOException {
            List<String> messages = new ArrayList<>();
            List<String> authors = new ArrayList<>();
            List<String> timestamps = new ArrayList<>();
            
            String line;
            while (!(line = reader.readLine()).equals("CHAT_HISTORY_END")) {
                String[] parts = line.split("\\|", 3);
                if (parts.length == 3) {
                    String timestamp = parts[0];
                    String author = parts[1];
                    String message = parts[2];
                    
                    timestamps.add(timestamp);
                    authors.add(author);
                    messages.add(message);
                }
            }
            
            // Send history to GUI for display
            gui.displayChatHistory(messages, authors, timestamps);
            logger.info("Received chat history with " + messages.size() + " messages");
        }

        private void handleFileHeader(String message) {
            String[] parts = message.split(":", 5);
            if (parts.length >= 5) {
                String fileName = parts[1];
                long fileSize = Long.parseLong(parts[2]);
                String fileType = parts[3];
                String fileDetails = parts[4];
                
                gui.displaySystemMessage("Đang nhận file: " + fileName + " (" + fileDetails + ")");
                logger.info("Receiving file: " + fileName + ", size: " + fileSize + ", type: " + fileType);
                
                // Create default path to save file
                String downloadsFolder = System.getProperty("user.home") + File.separator + "Downloads";
                String savePath = downloadsFolder + File.separator + fileName;
                
                // Create Downloads folder if it doesn't exist
                new File(downloadsFolder).mkdirs();
                
                // Accept file
                writer.println("ACCEPT_FILE:" + fileName + ":" + savePath);
                logger.info("Accepting file: " + fileName + " to be saved at: " + savePath);
            }
        }

        private void receiveFile(String fileName, String savePath) {
            try {
                InputStream is = socket.getInputStream();
                FileOutputStream fos = new FileOutputStream(savePath);
                
                // Get file size from server message
                String fileSizeMessage = reader.readLine();
                long fileSize = 0;
                if (fileSizeMessage.startsWith("FILE_SIZE:")) {
                    fileSize = Long.parseLong(fileSizeMessage.substring("FILE_SIZE:".length()));
                }
                
                // Notify server we're ready
                writer.println("READY_TO_RECEIVE:" + fileName);
                
                // Display receiving progress notification
                gui.displaySystemMessage("Đang tải file: " + fileName + " (0%)");
                
                // Receive file data
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytesReceived = 0;
                int lastProgressPercentage = 0;
                
                while (totalBytesReceived < fileSize && (bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesReceived += bytesRead;
                    
                    // Update progress
                    int progressPercentage = (int) ((totalBytesReceived * 100) / fileSize);
                    if (progressPercentage >= lastProgressPercentage + PROGRESS_UPDATE_INTERVAL) {
                        lastProgressPercentage = progressPercentage;
                        gui.updateFileProgress(fileName, progressPercentage);
                    }
                }
                
                fos.close();
                
                // Notify server file was received
                writer.println("FILE_RECEIVED:" + fileName);
                
                gui.updateFileProgress(fileName, 100);
                // Update UI when file download is complete
                gui.fileDownloadComplete(fileName, savePath);
                logger.info("File received successfully: " + fileName);
                
                // Copy the file to temp directory for preview
                try {
                    String tempPath = System.getProperty("java.io.tmpdir") + File.separator + 
                                     "chatclient_" + System.currentTimeMillis() + "_" + fileName;
                    Files.copy(Paths.get(savePath), Paths.get(tempPath));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error copying file to temp directory", e);
                }
                
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error receiving file", e);
                gui.displaySystemMessage("Lỗi khi nhận file: " + e.getMessage());
            }
        }

        private void processUserList(String userListString) {
            if (userListString.isEmpty()) return;

            String[] users = userListString.split(",");

            // Xóa danh sách cũ
            gui.clearUserList();

            // Thêm lại mỗi người dùng, loại bỏ người dùng hiện tại
            for (String user : users) {
                if (!user.isEmpty() && !user.equals(username)) {
                    gui.updateUserList(user, true);
                }
            }

            // Ghi log
            logger.info("Received user list: " + userListString);
        }
    }

    public void disconnect() {
        if (connected) {
            try {
                writer.println("LOGOUT");
                logger.info("Disconnected from server");
                connected = false;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during disconnect", e);
            } finally {
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
    
    public void forceLogin() {
        if (connected) {
            writer.println("FORCE_LOGIN");
            logger.info("Force login sent to server");
        }
    }
    
    // Để mở file với ứng dụng mặc định của hệ thống
    public void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    gui.displaySystemMessage("Không hỗ trợ mở file tự động trên hệ thống này.");
                }
            } else {
                gui.displaySystemMessage("File không tồn tại: " + filePath);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error opening file", e);
            gui.displaySystemMessage("Không thể mở file: " + e.getMessage());
        }
    }
    
    // Get file extension to determine how to handle it
    public String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    // Create preview for different file types
    public boolean canPreviewFile(String filename) {
        String ext = getFileExtension(filename);
        return ext.matches("jpg|jpeg|png|gif|bmp") || 
               ext.matches("pdf") ||
               ext.matches("mp3|wav|ogg") ||
               ext.matches("mp4|webm");
    }
}