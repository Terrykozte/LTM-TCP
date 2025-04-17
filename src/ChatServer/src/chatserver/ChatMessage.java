package chatserver;

public class ChatMessage {
    private String username;
    private String message;
    private String originalMessage;
    private String encryptedMessage;
    private long timestamp;
    
    public ChatMessage(String username, String message, long timestamp) {
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
        this.originalMessage = "";
        this.encryptedMessage = "";
    }
    
    public ChatMessage(String username, String message, String originalMessage, String encryptedMessage, long timestamp) {
        this.username = username;
        this.message = message;
        this.originalMessage = originalMessage;
        this.encryptedMessage = encryptedMessage;
        this.timestamp = timestamp;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getOriginalMessage() {
        return originalMessage;
    }
    
    public String getEncryptedMessage() {
        return encryptedMessage;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}