package chatserver;

public class VigenereCipher {
    private static final String DEFAULT_KEY = "CHATAPP";
    
    public String encrypt(String text) {
        return encrypt(text, DEFAULT_KEY);
    }
    
    public String decrypt(String text) {
        return decrypt(text, DEFAULT_KEY);
    }
    
    public String encrypt(String text, String key) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                boolean isUpperCase = Character.isUpperCase(c);
                char base = isUpperCase ? 'A' : 'a';
                char keyChar = Character.toUpperCase(key.charAt(j % key.length()));
                
                // Công thức mã hóa Vigenere
                int encryptedChar = (c - base + (keyChar - 'A')) % 26 + base;
                result.append((char) encryptedChar);
                j++;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    public String decrypt(String text, String key) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                boolean isUpperCase = Character.isUpperCase(c);
                char base = isUpperCase ? 'A' : 'a';
                char keyChar = Character.toUpperCase(key.charAt(j % key.length()));
                
                // Công thức giải mã Vigenere
                int decryptedChar = (c - base - (keyChar - 'A') + 26) % 26 + base;
                result.append((char) decryptedChar);
                j++;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}