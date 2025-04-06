package chatclient;

/**
 * Implements the Vigenere cipher for text encryption and decryption
 */
public class VigenereCipher {
    private static final String DEFAULT_KEY = "CHATAPP";
    
    /**
     * Encrypts a message using the default key
     * @param text The plaintext message to encrypt
     * @return The encrypted message
     */
    public String encrypt(String text) {
        return encrypt(text, DEFAULT_KEY);
    }
    
    /**
     * Decrypts a message using the default key
     * @param text The encrypted message to decrypt
     * @return The original plaintext message
     */
    public String decrypt(String text) {
        return decrypt(text, DEFAULT_KEY);
    }
    
    /**
     * Encrypts a message using a custom key
     * @param text The plaintext message to encrypt
     * @param key The encryption key
     * @return The encrypted message
     */
    public String encrypt(String text, String key) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                boolean isUpperCase = Character.isUpperCase(c);
                char base = isUpperCase ? 'A' : 'a';
                char keyChar = Character.toUpperCase(key.charAt(j % key.length()));
                
                // Vigenere encryption formula: (plaintext + key) % 26
                int encryptedChar = (c - base + (keyChar - 'A')) % 26 + base;
                result.append((char) encryptedChar);
                j++;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Decrypts a message using a custom key
     * @param text The encrypted message to decrypt
     * @param key The encryption key
     * @return The original plaintext message
     */
    public String decrypt(String text, String key) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                boolean isUpperCase = Character.isUpperCase(c);
                char base = isUpperCase ? 'A' : 'a';
                char keyChar = Character.toUpperCase(key.charAt(j % key.length()));
                
                // Vigenere decryption formula: (ciphertext - key + 26) % 26
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