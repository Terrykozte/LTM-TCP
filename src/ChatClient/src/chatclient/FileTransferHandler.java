package chatclient;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileTransferHandler {
    private static final Logger logger = Logger.getLogger(FileTransferHandler.class.getName());
    private Component parent;
    
    public FileTransferHandler(Component parent) {
        this.parent = parent;
    }
    
    public File selectFile(String fileType) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn " + getFileTypeDisplayName(fileType));
        
        // Customize file chooser appearance
        fileChooser.setPreferredSize(new Dimension(700, 500));
        
        // Thiết lập filter tùy theo loại file
        switch (fileType.toLowerCase()) {
            case "image":
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "Image Files", "jpg", "jpeg", "png", "gif", "bmp"));
                break;
            case "audio":
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "Audio Files", "mp3", "wav", "ogg", "aac", "flac"));
                break;
            case "video":
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "Video Files", "mp4", "avi", "mkv", "mov", "wmv"));
                break;
            case "document":
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "Document Files", "pdf", "doc", "docx", "txt", "rtf", "xls", "xlsx", "ppt", "pptx"));
                break;
            default:
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "All Files", "*"));
                break;
        }
        
        int result = fileChooser.showOpenDialog(parent);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            logger.log(Level.INFO, "Đã chọn file: " + selectedFile.getAbsolutePath());
            
            // Validate file size
            long fileSize = selectedFile.length();
            long maxSize = 100 * 1024 * 1024; // 100MB max
            
            if (fileSize > maxSize) {
                JOptionPane.showMessageDialog(parent,
                    "File quá lớn. Kích thước tối đa cho phép là 100MB.",
                    "Lỗi kích thước file",
                    JOptionPane.ERROR_MESSAGE);
                return null;
            }
            
            return selectedFile;
        }
        
        return null;
    }
    
    private String getFileTypeDisplayName(String fileType) {
        switch (fileType.toLowerCase()) {
            case "image": return "hình ảnh";
            case "audio": return "âm thanh";
            case "video": return "video";
            case "document": return "tài liệu";
            default: return "tệp tin";
        }
    }
    
    // Format file size for display
    public static String formatFileSize(long size) {
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
}