package chatclient;

public class VigenereCipher {
    private static final String DEFAULT_KEY = "SECRETKEY";
    
    private String key;
    
    public VigenereCipher() {
        this.key = DEFAULT_KEY;
    }
    
    public VigenereCipher(String key) {
        this.key = key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String encrypt(String text) {
        StringBuilder result = new StringBuilder();
        String upperText = text.toUpperCase();
        String upperKey = key.toUpperCase();
        
        int keyLength = upperKey.length();
        
        for (int i = 0; i < upperText.length(); i++) {
            char ch = upperText.charAt(i);
            
            if (Character.isLetter(ch)) {
                int textIndex = ch - 'A';
                int keyIndex = upperKey.charAt(i % keyLength) - 'A';
                int encryptedIndex = (textIndex + keyIndex) % 26;
                
                char encryptedChar = (char) (encryptedIndex + 'A');
                result.append(encryptedChar);
            } else {
                result.append(ch);
            }
        }
        
        return result.toString();
    }
    
    public String decrypt(String text) {
        StringBuilder result = new StringBuilder();
        String upperText = text.toUpperCase();
        String upperKey = key.toUpperCase();
        
        int keyLength = upperKey.length();
        
        for (int i = 0; i < upperText.length(); i++) {
            char ch = upperText.charAt(i);
            
            if (Character.isLetter(ch)) {
                int textIndex = ch - 'A';
                int keyIndex = upperKey.charAt(i % keyLength) - 'A';
                int decryptedIndex = (textIndex - keyIndex + 26) % 26;
                
                char decryptedChar = (char) (decryptedIndex + 'A');
                result.append(decryptedChar);
            } else {
                result.append(ch);
            }
        }
        
        return result.toString();
    }
}