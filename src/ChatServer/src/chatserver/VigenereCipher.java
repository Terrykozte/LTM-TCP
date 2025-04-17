package chatserver;

public class VigenereCipher {
    private static final String KEY = "SECRETKEY"; // Khóa mặc định
    
    public String encrypt(String text) {
        StringBuilder result = new StringBuilder();
        String key = KEY;
        
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (Character.isLetter(c)) {
                boolean isUpper = Character.isUpperCase(c);
                c = Character.toUpperCase(c);
                
                char keyChar = Character.toUpperCase(key.charAt(j % key.length()));
                j++;
                
                int shift = keyChar - 'A';
                char encryptedChar = (char) ((c - 'A' + shift) % 26 + 'A');
                
                result.append(isUpper ? encryptedChar : Character.toLowerCase(encryptedChar));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    public String decrypt(String text) {
        StringBuilder result = new StringBuilder();
        String key = KEY;
        
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (Character.isLetter(c)) {
                boolean isUpper = Character.isUpperCase(c);
                c = Character.toUpperCase(c);
                
                char keyChar = Character.toUpperCase(key.charAt(j % key.length()));
                j++;
                
                int shift = keyChar - 'A';
                char decryptedChar = (char) ((c - 'A' - shift + 26) % 26 + 'A');
                
                result.append(isUpper ? decryptedChar : Character.toLowerCase(decryptedChar));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}