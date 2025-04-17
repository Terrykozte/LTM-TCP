package chatclient;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.logging.*;

/**
 * Xử lý việc chọn và gửi file.
 */
public class FileTransferHandler {
    private final ChatClientGUI gui;
    private static final Logger logger = Logger.getLogger(FileTransferHandler.class.getName());
    
    public FileTransferHandler(ChatClientGUI gui) {
        this.gui = gui;
    }
    
    /**
     * Hiển thị hộp thoại chọn file cho người dùng.
     * 
     * @param type Loại file cần chọn ("image", "audio", "video", "document", "all")
     * @return File đã chọn hoặc null nếu người dùng hủy
     */
    public File selectFile(String type) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file để gửi");
        
        // Thiết lập bộ lọc dựa trên loại file
        switch (type.toLowerCase()) {
            case "image":
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Image Files", "jpg", "jpeg", "png", "gif", "bmp"));
                break;
            case "audio":
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Audio Files", "mp3", "wav", "ogg", "flac"));
                break;
            case "video":
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Video Files", "mp4", "avi", "mkv", "mov", "wmv"));
                break;
            case "document":
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                        "Document Files", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"));
                break;
            case "all":
                // Không thêm bộ lọc nào, cho phép tất cả các file
                break;
        }
        
        int result = fileChooser.showOpenDialog(gui);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Kiểm tra kích thước file (giới hạn 25MB)
            if (selectedFile.length() > 25 * 1024 * 1024) {
                JOptionPane.showMessageDialog(gui,
                        "File quá lớn! Kích thước tối đa là 25MB.",
                        "Lỗi kích thước file",
                        JOptionPane.ERROR_MESSAGE);
                logger.warning("File too large: " + selectedFile.getName() + " (" + selectedFile.length() + " bytes)");
                return null;
            }
            
            logger.info("Selected file: " + selectedFile.getAbsolutePath());
            return selectedFile;
        }
        
        return null;
    }
    
    /**
     * Xác định loại file dựa trên phần mở rộng.
     * 
     * @param fileName Tên file để kiểm tra
     * @return Loại file ("image", "audio", "video", "document", "other")
     */
    public static String getFileType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        
        if (extension.matches("jpg|jpeg|png|gif|bmp")) {
            return "image";
        } else if (extension.matches("mp3|wav|ogg|flac")) {
            return "audio";
        } else if (extension.matches("mp4|avi|mkv|mov|wmv")) {
            return "video";
        } else if (extension.matches("pdf|doc|docx|xls|xlsx|ppt|pptx|txt")) {
            return "document";
        } else {
            return "other";
        }
    }
}