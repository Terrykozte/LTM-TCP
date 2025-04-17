package chatclient;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.regex.Pattern;

public class ChatClientGUI extends JFrame {
    private JTextField tfServerIP, tfPort;
    private JTextArea taMessage;
    private JTextField tfUsername;
    private JPasswordField pfPassword, pfPasswordConfirm;
    private JTextPane tpChat;
    private JButton btnLogin, btnRegister, btnSwitchToRegister, btnSwitchToLogin, btnDisconnect, btnSend;
    private JButton btnAttachment;
    private JToggleButton btnShowPassword, btnShowPasswordConfirm;
    private JPanel loginPanel, registerPanel, chatPanel;
    private ChatClient client;
    private boolean isConnected = false;
    private static int clientCounter = 0;
    private static final Logger logger = Logger.getLogger(ChatClientGUI.class.getName());
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JLabel lblUserInfo, lblServerInfo, lblPortInfo, lblOnlineUsers;
    private LoginRegisterManager loginManager;
    private FileTransferHandler fileHandler;
    private List<String> onlineUsers = new ArrayList<>();
    private JPanel userListPanel;
    private JLabel passwordStrengthLabel;
    private JProgressBar passwordStrengthBar;
    private Map<String, Integer> fileProgressMap = new HashMap<>();
    private Map<String, Runnable> fileDownloadCallbacks = new ConcurrentHashMap<>();
    private Map<String, Component> fileComponentMap = new ConcurrentHashMap<>();
    private long lastMessageTime = 0;
    
    private StyledDocument chatDocument;
    private Style systemStyle, myMessageStyle, otherMessageStyle, joinLeaveStyle;

    private final Color PRIMARY_COLOR = new Color(63, 81, 181);
    private final Color PRIMARY_DARK_COLOR = new Color(48, 63, 159);
    private final Color ACCENT_COLOR = new Color(255, 64, 129);
    private final Color ACCENT_DARK_COLOR = new Color(200, 30, 85);
    private final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private final Color WARNING_COLOR = new Color(255, 152, 0);
    private final Color ERROR_COLOR = new Color(244, 67, 54);
    private final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private final Color TEXT_COLOR = new Color(33, 33, 33);
    private final Color LIGHT_TEXT = new Color(255, 255, 255);
    private final Color CHAT_BG = new Color(237, 241, 247);
    private final Color MY_MESSAGE_BG = new Color(63, 81, 181);
    private final Color OTHER_MESSAGE_BG = new Color(241, 241, 242);
    private final Color SYSTEM_MESSAGE_COLOR = new Color(117, 117, 117);
    private final Color JOIN_COLOR = new Color(76, 175, 80);
    private final Color LEAVE_COLOR = new Color(239, 83, 80);
    private final Color ATTACHMENT_COLOR = new Color(33, 150, 243);

    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private final Font SUB_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private final Font CHAT_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    
    private ImageIcon sendIcon;
    private ImageIcon attachmentIcon;
    private ImageIcon loginIcon;
    private ImageIcon registerIcon;
    private ImageIcon disconnectIcon;
    private ImageIcon appIcon;
    private ImageIcon logoIcon;
    private ImageIcon avatarIcon;
    private ImageIcon userListIcon;
    private ImageIcon showPasswordIcon;
    private ImageIcon hidePasswordIcon;
    private ImageIcon audioIcon;
    private ImageIcon videoIcon;
    private ImageIcon fileIcon;
    private ImageIcon imageIcon;
    private ImageIcon documentIcon;
    private ImageIcon downloadIcon;
    private ImageIcon viewIcon;
    private ImageIcon playIcon;

    private final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()\\-+=])(?=\\S+$).{8,}$");
    
    private final int MAX_CHARS_PER_LINE = 70;
    private final int CHAT_PANEL_WIDTH = 650;
    private final int USER_PANEL_WIDTH = 300;
    private final int INPUT_PANEL_HEIGHT = 120;
    
    private String tempFilesDir;

    public ChatClientGUI() {
        client = new ChatClient(this);
        loginManager = new LoginRegisterManager();
        fileHandler = new FileTransferHandler(this);
        clientCounter++;
        
        tempFilesDir = System.getProperty("java.io.tmpdir") + File.separator + "chatclient_" + System.currentTimeMillis() + "_" + clientCounter;
        createTempDirectory();
        
        loadIcons();
        setLookAndFeel();
        initComponents();
        setupLogger();
    }
    
    private void createTempDirectory() {
        try {
            Files.createDirectories(Paths.get(tempFilesDir));
            logger.info("Created temp directory: " + tempFilesDir);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not create temp directory", e);
            tempFilesDir = System.getProperty("java.io.tmpdir");
        }
    }
    
    public String getUsername() {
        return lblUserInfo.getText();
    }
    
    private void loadIcons() {
        createPlaceholderIcons();
    }
    
    private void createPlaceholderIcons() {
        sendIcon = new ImageIcon(createSendIcon(24, 24, LIGHT_TEXT));
        loginIcon = new ImageIcon(createLoginIcon(20, 20, LIGHT_TEXT));
        registerIcon = new ImageIcon(createRegisterIcon(20, 20, LIGHT_TEXT));
        disconnectIcon = new ImageIcon(createLogoutIcon(20, 20, LIGHT_TEXT));
        appIcon = new ImageIcon(createChatIcon(32, 32, PRIMARY_COLOR));
        logoIcon = new ImageIcon(createLogoIcon(120, 120, PRIMARY_COLOR));
        avatarIcon = new ImageIcon(createAvatarIcon(32, 32, new Color(158, 158, 158)));
        attachmentIcon = new ImageIcon(createAttachmentIcon(24, 24, LIGHT_TEXT));
        userListIcon = new ImageIcon(createUserListIcon(24, 24, LIGHT_TEXT));
        showPasswordIcon = new ImageIcon(createEyeIcon(20, 20, true));
        hidePasswordIcon = new ImageIcon(createEyeIcon(20, 20, false));
        audioIcon = new ImageIcon(createAudioIcon(24, 24, new Color(33, 150, 243)));
        videoIcon = new ImageIcon(createVideoIcon(24, 24, new Color(244, 67, 54)));
        fileIcon = new ImageIcon(createFileIcon(24, 24, new Color(158, 158, 158)));
        imageIcon = new ImageIcon(createImageIcon(24, 24, new Color(76, 175, 80)));
        documentIcon = new ImageIcon(createDocumentIcon(24, 24, new Color(255, 152, 0)));
        downloadIcon = new ImageIcon(createDownloadIcon(24, 24, new Color(33, 150, 243)));
        viewIcon = new ImageIcon(createViewIcon(24, 24, new Color(76, 175, 80)));
        playIcon = new ImageIcon(createPlayIcon(24, 24, new Color(244, 67, 54)));
    }

    private Image createSendIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        int[] xPoints = {2, width-4, 2};
        int[] yPoints = {4, height/2, height-4};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.dispose();
        return image;
    }

    private Image createLoginIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(width/4, 2, width/2, height-4);
        g2d.drawLine(2, height/2, width/2, height/2);
        g2d.fillPolygon(
            new int[]{width/2-2, width/2-6, width/2-6}, 
            new int[]{height/2, height/2-4, height/2+4}, 
            3);
        
        g2d.dispose();
        return image;
    }

    private Image createRegisterIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillOval(width/4, 2, width/2, height/2);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(width/2, height/2+2, width/2, height-2);
        g2d.drawLine(width/3, height*3/4, width*2/3, height*3/4);
        
        g2d.dispose();
        return image;
    }

    private Image createLogoutIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(width/4, 2, width/2, height-4);
        g2d.drawLine(width*3/4, height/2, width-2, height/2);
        g2d.fillPolygon(
            new int[]{width-2, width-6, width-6}, 
            new int[]{height/2, height/2-4, height/2+4}, 
            3);
        
        g2d.dispose();
        return image;
    }

    private Image createChatIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillRoundRect(2, 2, width-8, height-8, 10, 10);
        g2d.fillPolygon(
            new int[]{width-10, width-2, width-15}, 
            new int[]{height-8, height-2, height-2}, 
            3);
        
        g2d.setColor(Color.WHITE);
        g2d.drawLine(6, height/3, width-12, height/3);
        g2d.drawLine(6, height/2, width-12, height/2);
        g2d.drawLine(6, height*2/3, width/2, height*2/3);
        
        g2d.dispose();
        return image;
    }

    private Image createLogoIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillOval(width/4, height/4, width/2, height/2);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, width/4));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString("C", width/2 - fm.stringWidth("C")/2, height/2 + fm.getHeight()/4);
        
        g2d.dispose();
        return image;
    }

    private Image createAvatarIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillOval(2, 2, width-4, height-4);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(width/4, height/5, width/2, width/2);
        g2d.fillOval(width/4, height/2, width/2, height/2);
        
        g2d.dispose();
        return image;
    }

    private Image createAttachmentIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(width/2, 4, width/2, height-4);
        g2d.drawRoundRect(width/4, height/4, width/2, height/2, 5, 5);
        
        g2d.dispose();
        return image;
    }

    private Image createUserListIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillOval(width/4, 2, width/2, height/3);
        g2d.drawLine(width/2, height/3+2, width/2, height/2);
        g2d.drawLine(4, height*2/3, width-4, height*2/3);
        g2d.drawLine(4, height*4/5, width-4, height*4/5);
        g2d.drawLine(4, height-4, width*2/3, height-4);
        
        g2d.dispose();
        return image;
    }

    private Image createEyeIcon(int width, int height, boolean open) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawOval(2, 5, width - 4, height - 10);
        
        if (open) {
            g2d.fillOval(width/2 - 2, height/2 - 2, 4, 4);
        } else {
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(4, height/2, width - 4, height/2);
        }
        
        g2d.dispose();
        return image;
    }

    private Image createAudioIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillRect(4, height/3, width/3, height/3);
        int[] xPoints = {width/3+4, width*2/3, width*2/3, width/3+4};
        int[] yPoints = {height/3, height/6, height*5/6, height*2/3};
        g2d.fillPolygon(xPoints, yPoints, 4);
        
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawArc(width*2/3-2, height/4, width/6, height/2, -70, 140);
        g2d.drawArc(width*2/3, height/8, width/5, height*3/4, -70, 140);
        
        g2d.dispose();
        return image;
    }

    private Image createVideoIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillRoundRect(2, 4, width*2/3, height-8, 5, 5);
        int[] xPoints = {width*2/3+3, width-2, width*2/3+3};
        int[] yPoints = {height/4, height/2, height*3/4};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.dispose();
        return image;
    }

    private Image createFileIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillRect(4, 2, width*3/4, height-4);
        g2d.setColor(color.darker());
        int[] xPoints = {width*3/4+4, width*3/4+4, width-2};
        int[] yPoints = {2, height/4, height/4};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.setColor(Color.WHITE);
        g2d.drawLine(8, height/3, width*3/4, height/3);
        g2d.drawLine(8, height/2, width*3/4, height/2);
        g2d.drawLine(8, height*2/3, width*2/3, height*2/3);
        
        g2d.dispose();
        return image;
    }

    private Image createImageIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillRect(2, 2, width-4, height-4);
        
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(width/4, height/4, width/6, width/6);
        
        g2d.setColor(new Color(139, 69, 19));
        int[] xPoints = {2, width/3, width*2/3};
        int[] yPoints = {height-4, height/2, height-4};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        int[] xPoints2 = {width/2, width-4, width-4};
        int[] yPoints2 = {height/3, height-4, height/2};
        g2d.fillPolygon(xPoints2, yPoints2, 3);
        
        g2d.dispose();
        return image;
    }

    private Image createDocumentIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        g2d.fillRect(4, 2, width-8, height-4);
        
        g2d.setColor(Color.WHITE);
        g2d.drawLine(8, height/4, width-12, height/4);
        g2d.drawLine(8, height/2, width-12, height/2);
        g2d.drawLine(8, height*3/4, width-12, height*3/4);
        
        g2d.dispose();
        return image;
    }

    private Image createDownloadIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(width/2, 4, width/2, height*2/3);
        g2d.fillPolygon(
            new int[]{width/2, width/2-6, width/2+6}, 
            new int[]{height*2/3, height*2/3-8, height*2/3-8}, 
            3);
        
        g2d.drawLine(width/5, height-4, width*4/5, height-4);
        
        g2d.dispose();
        return image;
    }

    private Image createViewIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        
        g2d.setStroke(new BasicStroke(2));
        g2d.drawArc(4, height/4, width-8, height/2, 0, 180);
        g2d.drawArc(4, height/4, width-8, height/2, 180, 180);
        
        g2d.fillOval(width/2-3, height/2-3, 6, 6);
        
        g2d.dispose();
        return image;
    }

    private Image createPlayIcon(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(color);
        
        int[] xPoints = {4, width-4, 4};
        int[] yPoints = {4, height/2, height-4};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.dispose();
        return image;
    }
    
    private ImageIcon createScaledIcon(String path, int width, int height) {
        try {
            ImageIcon originalIcon = new ImageIcon(getClass().getResource(path));
            Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load icon: " + path, e);
            return null;
        }
    }
    
    private void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    
                    UIManager.put("nimbusBase", PRIMARY_COLOR);
                    UIManager.put("nimbusBlueGrey", BACKGROUND_COLOR);
                    UIManager.put("control", BACKGROUND_COLOR);
                    UIManager.put("text", TEXT_COLOR);
                    UIManager.put("nimbusLightBackground", Color.WHITE);
                    UIManager.put("info", Color.WHITE);
                    
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not set look and feel", ex);
            }
        }
    }

    private void setupLogger() {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdir();
            }
            
            Handler fileHandler = new FileHandler("logs/chatclient_" + clientCounter + ".log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(fileHandler);
            Logger.getLogger("").setLevel(Level.INFO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(LIGHT_TEXT);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(color.darker().darker());
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(color.darker());
            }
        });
        
        return button;
    }
    
    private JButton createIconButton(String text, ImageIcon icon, Color color) {
        JButton button = createStyledButton(text, color);
        if (icon != null) {
            button.setIcon(icon);
            button.setIconTextGap(8);
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
        }
        return button;
    }
    
    private JPanel createStyledTextField(String placeholder, JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel label = new JLabel(placeholder);
        label.setFont(NORMAL_FONT);
        label.setForeground(PRIMARY_COLOR);
        
        textField.setFont(NORMAL_FONT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        textField.setOpaque(false);
        
        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createStyledPasswordField(String placeholder, JPasswordField passwordField, 
                                            JToggleButton toggleButton) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel label = new JLabel(placeholder);
        label.setFont(NORMAL_FONT);
        label.setForeground(PRIMARY_COLOR);
        
        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.setOpaque(false);
        
        passwordField.setFont(NORMAL_FONT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        passwordField.setOpaque(false);
        
        toggleButton.setIcon(hidePasswordIcon);
        toggleButton.setSelectedIcon(showPasswordIcon);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.setToolTipText("Hiển thị/Ẩn mật khẩu");
        
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });
        
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(toggleButton, BorderLayout.EAST);
        
        panel.add(label, BorderLayout.NORTH);
        panel.add(passwordPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRoundedPanel(Color bgColor) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBackground(bgColor);
        return panel;
    }
    
    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, 
                message, 
                "Lỗi", 
                JOptionPane.ERROR_MESSAGE);
    }
    
    private void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(this, 
                message, 
                "Thành công", 
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    private boolean showConfirmDialog(String message, String title) {
        int result = JOptionPane.showConfirmDialog(
                this, 
                message, 
                title, 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        return result == JOptionPane.YES_OPTION;
    }

    private void initComponents() {
        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(950, 700);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }
        });
        
        if (appIcon != null) {
            setIconImage(appIcon.getImage());
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BACKGROUND_COLOR);
        getContentPane().add(mainPanel);

        loginPanel = createLoginPanel();
        registerPanel = createRegisterPanel();
        chatPanel = createChatPanel();

        mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");
        mainPanel.add(chatPanel, "chat");

        cardLayout.show(mainPanel, "login");
    }
    
    private void confirmAndExit() {
        if (isConnected) {
            if (showConfirmDialog(
                    "Bạn đang kết nối đến server. Bạn có chắc chắn muốn thoát?", 
                    "Xác nhận thoát")) {
                client.disconnect();
                dispose();
                System.exit(0);
            }
        } else {
            if (showConfirmDialog(
                    "Bạn có chắc chắn muốn thoát?", 
                    "Xác nhận thoát")) {
                dispose();
                System.exit(0);
            }
        }
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
           
        JLabel lblTitle = new JLabel("CHAT CLIENT");
        lblTitle.setFont(HEADER_FONT);
        lblTitle.setForeground(PRIMARY_COLOR);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);
        
        JLabel lblSubtitle = new JLabel("Đăng nhập để bắt đầu trò chuyện");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblSubtitle.setForeground(TEXT_COLOR);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblSubtitle);
        
        panel.add(headerPanel);
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(220, 220, 220), 12),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));
        formPanel.setMaximumSize(new Dimension(450, 400));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        tfUsername = new JTextField(20);
        JPanel usernamePanel = createStyledTextField("Tên đăng nhập", tfUsername);
        formPanel.add(usernamePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        pfPassword = new JPasswordField(20);
        btnShowPassword = new JToggleButton();
        JPanel passwordPanel = createStyledPasswordField("Mật khẩu", pfPassword, btnShowPassword);
        formPanel.add(passwordPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.X_AXIS));
        serverPanel.setOpaque(false);
        
        tfServerIP = new JTextField("localhost", 20);
        JPanel serverIPPanel = createStyledTextField("Địa chỉ server", tfServerIP);
        serverIPPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        tfPort = new JTextField("12345", 20);
        JPanel portPanel = createStyledTextField("Cổng", tfPort);
        portPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        portPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        serverPanel.add(serverIPPanel);
        serverPanel.add(portPanel);
        
        formPanel.add(serverPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        btnLogin = createIconButton("ĐĂNG NHẬP", loginIcon, PRIMARY_COLOR);
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLogin.addActionListener(e -> handleLogin());
        
        ActionListener loginAction = e -> handleLogin();
        tfUsername.addActionListener(loginAction);
        pfPassword.addActionListener(loginAction);
        tfServerIP.addActionListener(loginAction);
        tfPort.addActionListener(loginAction);
        
        formPanel.add(btnLogin);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPanel registerLinkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        registerLinkPanel.setOpaque(false);
        
        JLabel lblNoAccount = new JLabel("Chưa có tài khoản? ");
        lblNoAccount.setFont(NORMAL_FONT);
        
        btnSwitchToRegister = new JButton("Đăng ký ngay");
        btnSwitchToRegister.setFont(BUTTON_FONT);
        btnSwitchToRegister.setForeground(PRIMARY_COLOR);
        btnSwitchToRegister.setBorderPainted(false);
        btnSwitchToRegister.setContentAreaFilled(false);
        btnSwitchToRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSwitchToRegister.addActionListener(e -> {
            cardLayout.show(mainPanel, "register");
        });
        
        registerLinkPanel.add(lblNoAccount);
        registerLinkPanel.add(btnSwitchToRegister);
        
        formPanel.add(registerLinkPanel);
        
        panel.add(formPanel);
        
        return panel;
    }
    
    private class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        
        public RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }
        
        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
    
    private void handleLogin() {
        String username = tfUsername.getText().trim();
        String password = new String(pfPassword.getPassword());
        String serverIP = tfServerIP.getText().trim();
        String portText = tfPort.getText().trim();
        
        if (username.isEmpty() || password.isEmpty() || serverIP.isEmpty() || portText.isEmpty()) {
            showErrorMessage("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        
        try {
            int port = Integer.parseInt(portText);
            
            if (port < 1024 || port > 65535) {
                showErrorMessage("Port không hợp lệ! Port phải từ 1024 đến 65535.");
                return;
            }
            
            boolean loginValid = loginManager.login(username, password);
            
            if (!loginValid) {
                showErrorMessage("Thông tin đăng nhập không chính xác!");
                return;
            }
            
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            btnLogin.setEnabled(false);
            btnLogin.setText("Đang kết nối...");
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return client.connect(serverIP, port, username);
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            isConnected = true;
                            lblUserInfo.setText(username);
                            lblServerInfo.setText("Server: " + serverIP);
                            lblPortInfo.setText("Port: " + port);
                            cardLayout.show(mainPanel, "chat");
                            setTitle("Chat Client - " + username);
                            updateUserList(username, true);
                            displaySystemMessage("Đã kết nối đến server " + serverIP + " qua port " + port + "!");
                            taMessage.requestFocus();
                            
                            requestChatHistory();
                        } else {
                            showErrorMessage("Không thể kết nối đến server!");
                        }
                    } catch (Exception ex) {
                        showErrorMessage("Lỗi kết nối: " + ex.getMessage());
                        logger.log(Level.SEVERE, "Connection error", ex);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                        btnLogin.setEnabled(true);
                        btnLogin.setText("ĐĂNG NHẬP");
                    }
                }
            };
            
            worker.execute();
        } catch (NumberFormatException ex) {
            showErrorMessage("Port không hợp lệ! Vui lòng nhập số.");
        }
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel lblTitle = new JLabel("TẠO TÀI KHOẢN MỚI");
        lblTitle.setFont(HEADER_FONT);
        lblTitle.setForeground(PRIMARY_COLOR);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);
        
        JLabel lblSubtitle = new JLabel("Vui lòng nhập thông tin đăng ký");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblSubtitle.setForeground(TEXT_COLOR);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblSubtitle);
        
        panel.add(headerPanel);
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(220, 220, 220), 12),
                BorderFactory.createEmptyBorder(25, 40, 25, 40)));
        formPanel.setMaximumSize(new Dimension(450, 500));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField tfRegUsername = new JTextField(20);
        JPanel usernamePanel = createStyledTextField("Tên đăng nhập", tfRegUsername);
        formPanel.add(usernamePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPasswordField pfRegPassword = new JPasswordField(20);
        btnShowPasswordConfirm = new JToggleButton();
        JPanel passwordPanel = createStyledPasswordField("Mật khẩu", pfRegPassword, btnShowPasswordConfirm);
        formPanel.add(passwordPanel);
        
        JPanel strengthPanel = new JPanel(new BorderLayout(10, 0));
        strengthPanel.setOpaque(false);
        
        passwordStrengthLabel = new JLabel("Độ mạnh: Chưa nhập");
        passwordStrengthLabel.setFont(SMALL_FONT);
        passwordStrengthLabel.setForeground(SYSTEM_MESSAGE_COLOR);
        
        passwordStrengthBar = new JProgressBar(0, 100);
        passwordStrengthBar.setValue(0);
        passwordStrengthBar.setStringPainted(false);
        passwordStrengthBar.setPreferredSize(new Dimension(100, 5));
        
        strengthPanel.add(passwordStrengthLabel, BorderLayout.WEST);
        strengthPanel.add(passwordStrengthBar, BorderLayout.CENTER);
        
        formPanel.add(strengthPanel);
        
        JLabel passwordReqLabel = new JLabel("<html>Mật khẩu phải có ít nhất 8 kí tự, bao gồm: chữ hoa, chữ thường, số và kí tự đặc biệt (!@#$%^&*()-+=)</html>");
        passwordReqLabel.setFont(SMALL_FONT);
        passwordReqLabel.setForeground(SYSTEM_MESSAGE_COLOR);
        passwordReqLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));
        formPanel.add(passwordReqLabel);
        
        pfRegPassword.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updatePasswordStrength(new String(pfRegPassword.getPassword()));
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updatePasswordStrength(new String(pfRegPassword.getPassword()));
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updatePasswordStrength(new String(pfRegPassword.getPassword()));
            }
        });
        
        pfPasswordConfirm = new JPasswordField(20);
        JToggleButton btnShowConfirmPassword = new JToggleButton();
        JPanel confirmPasswordPanel = createStyledPasswordField("Xác nhận mật khẩu", pfPasswordConfirm, btnShowConfirmPassword);
        formPanel.add(confirmPasswordPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        
        btnRegister = createIconButton("ĐĂNG KÝ", registerIcon, ACCENT_COLOR);
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnRegister.addActionListener(e -> {
            String username = tfRegUsername.getText().trim();
            String password = new String(pfRegPassword.getPassword());
            String confirmPassword = new String(pfPasswordConfirm.getPassword());
            
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showErrorMessage("Vui lòng nhập đầy đủ thông tin!");
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                showErrorMessage("Mật khẩu xác nhận không khớp!");
                return;
            }
            
            if (username.length() < 3) {
                showErrorMessage("Tên người dùng phải có ít nhất 3 ký tự!");
                return;
            }
            
            if (password.length() < 8) {
                showErrorMessage("Mật khẩu phải có ít nhất 8 ký tự!");
                return;
            }
            
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                showErrorMessage("Mật khẩu không đủ mạnh! Mật khẩu phải có ít nhất 8 kí tự, bao gồm: chữ hoa, chữ thường, số và kí tự đặc biệt.");
                return;
            }
            
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            btnRegister.setEnabled(false);
            btnRegister.setText("Đang xử lý...");
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return loginManager.register(username, password);
                }
                
                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        
                        if (success) {
                            showSuccessMessage("Đăng ký thành công!");
                            
                            tfUsername.setText(username);
                            pfPassword.setText(password);
                            cardLayout.show(mainPanel, "login");
                        } else {
                            showErrorMessage("Tên người dùng đã tồn tại!");
                        }
                    } catch (Exception ex) {
                        showErrorMessage("Lỗi đăng ký: " + ex.getMessage());
                        logger.log(Level.SEVERE, "Registration error", ex);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                        btnRegister.setEnabled(true);
                        btnRegister.setText("ĐĂNG KÝ");
                    }
                }
            };
            
            worker.execute();
        });
        
        ActionListener registerAction = e -> btnRegister.doClick();
        tfRegUsername.addActionListener(registerAction);
        pfRegPassword.addActionListener(registerAction);
        pfPasswordConfirm.addActionListener(registerAction);
        
        formPanel.add(btnRegister);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPanel loginLinkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginLinkPanel.setOpaque(false);
        
        JLabel lblHaveAccount = new JLabel("Đã có tài khoản? ");
        lblHaveAccount.setFont(NORMAL_FONT);
        
        btnSwitchToLogin = new JButton("Đăng nhập ngay");
        btnSwitchToLogin.setFont(BUTTON_FONT);
        btnSwitchToLogin.setForeground(PRIMARY_COLOR);
        btnSwitchToLogin.setBorderPainted(false);
        btnSwitchToLogin.setContentAreaFilled(false);
        btnSwitchToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSwitchToLogin.addActionListener(e -> {
            cardLayout.show(mainPanel, "login");
        });
        
        loginLinkPanel.add(lblHaveAccount);
        loginLinkPanel.add(btnSwitchToLogin);
        
        formPanel.add(loginLinkPanel);
        
        panel.add(formPanel);
        
        return panel;
    }
    
    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthLabel.setText("Độ mạnh: Chưa nhập");
            passwordStrengthLabel.setForeground(SYSTEM_MESSAGE_COLOR);
            passwordStrengthBar.setValue(0);
            passwordStrengthBar.setForeground(SYSTEM_MESSAGE_COLOR);
            return;
        }
        
        int strength = calculatePasswordStrength(password);
        passwordStrengthBar.setValue(strength);
        
        if (strength < 40) {
            passwordStrengthLabel.setText("Độ mạnh: Yếu");
            passwordStrengthLabel.setForeground(ERROR_COLOR);
            passwordStrengthBar.setForeground(ERROR_COLOR);
        } else if (strength < 70) {
            passwordStrengthLabel.setText("Độ mạnh: Trung bình");
            passwordStrengthLabel.setForeground(WARNING_COLOR);
            passwordStrengthBar.setForeground(WARNING_COLOR);
        } else {
            passwordStrengthLabel.setText("Độ mạnh: Mạnh");
            passwordStrengthLabel.setForeground(SUCCESS_COLOR);
            passwordStrengthBar.setForeground(SUCCESS_COLOR);
        }
    }
    
    private int calculatePasswordStrength(String password) {
        int score = 0;
        
        if (password.length() >= 8) score += 20;
        else if (password.length() >= 6) score += 10;
        
        if (password.matches(".*[a-z].*")) score += 10;
        
        if (password.matches(".*[A-Z].*")) score += 15;
        
        if (password.matches(".*[0-9].*")) score += 15;
        
        if (password.matches(".*[!@#$%^&*()\\-+=].*")) score += 20;
        
        if (password.matches(".*[A-Za-z].*") && 
            password.matches(".*[0-9].*") && 
            password.matches(".*[!@#$%^&*()\\-+=].*")) {
            score += 20;
        }
        
        return Math.min(score, 100);
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BACKGROUND_COLOR);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        infoPanel.setOpaque(false);
        
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userPanel.setOpaque(false);
        
        JLabel lblAvatar = new JLabel(avatarIcon);
        userPanel.add(lblAvatar);
        
        lblUserInfo = new JLabel("Username");
        lblUserInfo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblUserInfo.setForeground(LIGHT_TEXT);
        userPanel.add(lblUserInfo);
        
        infoPanel.add(userPanel);
        
        JPanel serverDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        serverDetailsPanel.setOpaque(false);
        
        lblServerInfo = new JLabel("Server: N/A");
        lblServerInfo.setFont(NORMAL_FONT);
        lblServerInfo.setForeground(LIGHT_TEXT);
        serverDetailsPanel.add(lblServerInfo);
        
        lblPortInfo = new JLabel("Port: N/A");
        lblPortInfo.setFont(NORMAL_FONT);
        lblPortInfo.setForeground(LIGHT_TEXT);
        serverDetailsPanel.add(lblPortInfo);
        
        lblOnlineUsers = new JLabel("0 người dùng trực tuyến");
        lblOnlineUsers.setFont(NORMAL_FONT);
        lblOnlineUsers.setForeground(LIGHT_TEXT);
        lblOnlineUsers.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblOnlineUsers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showOnlineUsersList();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                lblOnlineUsers.setText("<html><u>" + lblOnlineUsers.getText().replace("<html><u>", "").replace("</u></html>", "") + "</u></html>");
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                lblOnlineUsers.setText(lblOnlineUsers.getText().replace("<html><u>", "").replace("</u></html>", ""));
            }
        });
        lblOnlineUsers.setIcon(userListIcon);
        serverDetailsPanel.add(lblOnlineUsers);
        
        infoPanel.add(serverDetailsPanel);
        
        headerPanel.add(infoPanel, BorderLayout.WEST);
        
        btnDisconnect = createIconButton("Đăng xuất", disconnectIcon, ERROR_COLOR);
        btnDisconnect.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDisconnect.addActionListener(e -> {
            if (showConfirmDialog(
                    "Bạn có chắc chắn muốn đăng xuất?", 
                    "Xác nhận đăng xuất")) {
                disconnectFromServer();
            }
        });
        
        headerPanel.add(btnDisconnect, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setDividerLocation(getWidth() - USER_PANEL_WIDTH - 20);
        mainSplitPane.setBorder(null);
        mainSplitPane.setResizeWeight(1.0);
        mainSplitPane.setOneTouchExpandable(false);
        
        JPanel usersContainerPanel = new JPanel(new BorderLayout());
        usersContainerPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
        usersContainerPanel.setBackground(BACKGROUND_COLOR);
        usersContainerPanel.setPreferredSize(new Dimension(USER_PANEL_WIDTH, 0));
        
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(Color.WHITE);
        userListPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel lblUsersHeader = new JLabel("NGƯỜI DÙNG TRỰC TUYẾN");
        lblUsersHeader.setFont(SUB_HEADER_FONT);
        lblUsersHeader.setForeground(PRIMARY_COLOR);
        lblUsersHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        lblUsersHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        userListPanel.add(lblUsersHeader);
        
        JScrollPane userListScrollPane = new JScrollPane(userListPanel);
        userListScrollPane.setBorder(BorderFactory.createEmptyBorder());
        userListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        userListScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        usersContainerPanel.add(userListScrollPane, BorderLayout.CENTER);
        
        JPanel chatContainerPanel = new JPanel(new BorderLayout());
        chatContainerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
        chatContainerPanel.setBackground(CHAT_BG);
        
        tpChat = new JTextPane();
        tpChat.setEditable(false);
        tpChat.setBackground(Color.WHITE);
        tpChat.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        chatDocument = tpChat.getStyledDocument();
        
        systemStyle = tpChat.addStyle("System", null);
        StyleConstants.setForeground(systemStyle, SYSTEM_MESSAGE_COLOR);
        StyleConstants.setFontFamily(systemStyle, "Segoe UI");
        StyleConstants.setFontSize(systemStyle, 13);
        StyleConstants.setAlignment(systemStyle, StyleConstants.ALIGN_CENTER);
        StyleConstants.setItalic(systemStyle, true);
        
        joinLeaveStyle = tpChat.addStyle("JoinLeave", null);
        StyleConstants.setForeground(joinLeaveStyle, JOIN_COLOR);
        StyleConstants.setFontFamily(joinLeaveStyle, "Segoe UI");
        StyleConstants.setFontSize(joinLeaveStyle, 13);
        StyleConstants.setAlignment(joinLeaveStyle, StyleConstants.ALIGN_CENTER);
        StyleConstants.setItalic(joinLeaveStyle, true);
        
        myMessageStyle = tpChat.addStyle("Me", null);
        StyleConstants.setForeground(myMessageStyle, LIGHT_TEXT);
        StyleConstants.setFontFamily(myMessageStyle, "Segoe UI");
        StyleConstants.setFontSize(myMessageStyle, 14);
        StyleConstants.setBold(myMessageStyle, true);
        StyleConstants.setAlignment(myMessageStyle, StyleConstants.ALIGN_RIGHT);
        
        otherMessageStyle = tpChat.addStyle("Other", null);
        StyleConstants.setForeground(otherMessageStyle, TEXT_COLOR);
        StyleConstants.setFontFamily(otherMessageStyle, "Segoe UI");
        StyleConstants.setFontSize(otherMessageStyle, 14);
        StyleConstants.setAlignment(otherMessageStyle, StyleConstants.ALIGN_LEFT);
        
        JScrollPane chatScrollPane = new JScrollPane(tpChat);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        tpChat.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repaintChatAndAdjustWidths();
            }
        });
        
        chatContainerPanel.add(chatScrollPane, BorderLayout.CENTER);
        
        mainSplitPane.setLeftComponent(chatContainerPanel);
        mainSplitPane.setRightComponent(usersContainerPanel);
        
        mainSplitPane.setUI(new BasicSplitPaneUI() {
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(BACKGROUND_COLOR);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });
        
        panel.add(mainSplitPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        inputPanel.setPreferredSize(new Dimension(0, INPUT_PANEL_HEIGHT));
        
        JPanel messagePanel = new JPanel(new BorderLayout(10, 0));
        messagePanel.setOpaque(false);
        
        taMessage = new JTextArea(3, 20);
        taMessage.setFont(NORMAL_FONT);
        taMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        taMessage.setLineWrap(true);
        taMessage.setWrapStyleWord(true);

        taMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    sendMessage();
                    e.consume();
                }
            }
        });
        
        JScrollPane messageScrollPane = new JScrollPane(taMessage);
        messageScrollPane.setBorder(BorderFactory.createEmptyBorder());
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        messageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        messagePanel.add(messageScrollPane, BorderLayout.CENTER);
        
        btnAttachment = new JButton("Đính kèm");
        btnAttachment.setIcon(attachmentIcon);
        btnAttachment.setBackground(ATTACHMENT_COLOR);
        btnAttachment.setForeground(LIGHT_TEXT);
        btnAttachment.setFont(BUTTON_FONT);
        btnAttachment.setFocusPainted(false);
        btnAttachment.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAttachment.setPreferredSize(new Dimension(120, 40));
        btnAttachment.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        btnAttachment.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnAttachment.setBackground(ATTACHMENT_COLOR.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btnAttachment.setBackground(ATTACHMENT_COLOR);
            }
        });
        
        btnAttachment.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("all");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        btnSend = createIconButton("Gửi", sendIcon, PRIMARY_COLOR);
        btnSend.setBackground(PRIMARY_COLOR);
        btnSend.setForeground(LIGHT_TEXT);
        btnSend.setPreferredSize(new Dimension(100, 40));
        btnSend.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btnSend.setFocusPainted(false);
        btnSend.setToolTipText("Gửi tin nhắn (Enter)");
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setFont(BUTTON_FONT);
        btnSend.addActionListener(e -> {
            sendMessage();
        });
        
        btnSend.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnSend.setBackground(PRIMARY_DARK_COLOR);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btnSend.setBackground(PRIMARY_COLOR);
            }
        });
        
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(btnAttachment);
        buttonsPanel.add(btnSend);
        buttonsPanel.setPreferredSize(new Dimension(240, 40));
        
        messagePanel.add(buttonsPanel, BorderLayout.EAST);
        
        inputPanel.add(messagePanel, BorderLayout.CENTER);
        
        panel.add(inputPanel, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                mainSplitPane.setDividerLocation(getWidth() - USER_PANEL_WIDTH - 40);
            }
        });

        return panel;
    }
    
    private void repaintChatAndAdjustWidths() {
        SwingUtilities.invokeLater(() -> {
            int chatWidth = tpChat.getWidth();
            if (chatWidth <= 0) return;
        });
    }
    
    private void displayCenterTime(String timeStamp) {
        try {
            int start = chatDocument.getLength();
            chatDocument.insertString(start, "\n", null);
            
            JPanel timePanel = new JPanel();
            timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.Y_AXIS));
            timePanel.setOpaque(false);
            
            JLabel timeLabel = new JLabel("------------ " + timeStamp + " ------------");
            timeLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            timeLabel.setForeground(Color.GRAY);
            timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            timePanel.add(Box.createVerticalStrut(10));
            timePanel.add(timeLabel);
            timePanel.add(Box.createVerticalStrut(10));
            
            Style style = chatDocument.addStyle("CenterTimeStyle", null);
            StyleConstants.setComponent(style, timePanel);
            
            start = chatDocument.getLength();
            chatDocument.insertString(start, " ", style);
            
            SimpleAttributeSet center = new SimpleAttributeSet();
            StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
            chatDocument.setParagraphAttributes(start, 1, center, false);
            
            chatDocument.insertString(start + 1, "\n", null);
        } catch (BadLocationException e) {
            logger.log(Level.WARNING, "Error displaying center time", e);
        }
    }
    
    public void updateFileProgress(String fileName, int progress) {
        SwingUtilities.invokeLater(() -> {
            fileProgressMap.put(fileName, progress);
            String progressText = "Đang gửi file: " + fileName + " (" + progress + "%)";
            
            try {
                Document doc = tpChat.getDocument();
                String text = doc.getText(0, doc.getLength());
                String filePattern = "Đang gửi file: " + fileName;
                
                int lastIndex = text.lastIndexOf(filePattern);
                if (lastIndex >= 0) {
                    int endOfLine = text.indexOf("\n", lastIndex);
                    if (endOfLine < 0) endOfLine = text.length();
                    
                    doc.remove(lastIndex, endOfLine - lastIndex);
                    doc.insertString(lastIndex, progressText, null);
                } else {
                    displaySystemMessage(progressText);
                }
            } catch (BadLocationException e) {
                logger.log(Level.WARNING, "Error updating file progress", e);
            }
        });
    }
    
    public void requestOnlineUsers() {
        if (client != null && client.isConnected()) {
            client.requestOnlineUsers();
        }
    }
    
    private void showOnlineUsersList() {
        requestOnlineUsers();
        
        JDialog userDialog = new JDialog(this, "Người dùng trực tuyến", true);
        userDialog.setSize(300, 400);
        userDialog.setLocationRelativeTo(this);
        userDialog.setLayout(new BorderLayout());
        
        JPanel usersPanel = new JPanel();
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setBackground(Color.WHITE);
        usersPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel headerLabel = new JLabel("NGƯỜI DÙNG TRỰC TUYẾN (" + onlineUsers.size() + ")");
        headerLabel.setFont(SUB_HEADER_FONT);
        headerLabel.setForeground(PRIMARY_COLOR);
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        usersPanel.add(headerLabel);
        
        for (String user : onlineUsers) {
            JPanel userPanel = new JPanel(new BorderLayout());
            userPanel.setBackground(Color.WHITE);
            userPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                    BorderFactory.createEmptyBorder(10, 5, 10, 5)));
            
            JLabel userLabel = new JLabel(user);
            userLabel.setFont(NORMAL_FONT);
            
            userLabel.setIcon(avatarIcon);
            userLabel.setIconTextGap(10);
            
            userPanel.add(userLabel, BorderLayout.WEST);
            
            JPanel statusPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(SUCCESS_COLOR);
                    g2d.fillOval(0, 0, 10, 10);
                }
            };
            statusPanel.setOpaque(false);
            statusPanel.setPreferredSize(new Dimension(10, 10));
            
            JPanel indicatorContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            indicatorContainer.setOpaque(false);
            indicatorContainer.add(statusPanel);
            
            userPanel.add(indicatorContainer, BorderLayout.EAST);
            
            userPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, userPanel.getPreferredSize().height));
            usersPanel.add(userPanel);
        }
        
        JScrollPane scrollPane = new JScrollPane(usersPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        userDialog.add(scrollPane, BorderLayout.CENTER);
        
        JButton closeButton = createStyledButton("Đóng", PRIMARY_COLOR);
        closeButton.addActionListener(e -> userDialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(closeButton);
        
        userDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        userDialog.setVisible(true);
    }
    
    public void updateUserList(String username, boolean isJoining) {
        SwingUtilities.invokeLater(() -> {
            if (isJoining) {
                if (!onlineUsers.contains(username)) {
                    onlineUsers.add(username);
                }
            } else {
                onlineUsers.remove(username);
            }

            String currentUser = getUsername();
            if (!onlineUsers.contains(currentUser) && !currentUser.equals("Username")) {
                onlineUsers.add(currentUser);
            }

            lblOnlineUsers.setText(onlineUsers.size() + " người dùng trực tuyến");
            updateUserListPanel();
        });
    }

    private void updateUserListPanel() {
        userListPanel.removeAll();

        JLabel lblUsersHeader = new JLabel("NGƯỜI DÙNG TRỰC TUYẾN (" + onlineUsers.size() + ")");
        lblUsersHeader.setFont(SUB_HEADER_FONT);
        lblUsersHeader.setForeground(PRIMARY_COLOR);
        lblUsersHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        lblUsersHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        userListPanel.add(lblUsersHeader);
        userListPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        String currentUser = getUsername();
        if (onlineUsers.contains(currentUser)) {
            JPanel userPanel = createUserPanel(currentUser, true);
            userListPanel.add(userPanel);
        }

        for (String user : onlineUsers) {
            if (!user.equals(currentUser)) {
                JPanel userPanel = createUserPanel(user, false);
                userListPanel.add(userPanel);
            }
        }

        userListPanel.revalidate();
        userListPanel.repaint();
    }

    private JPanel createUserPanel(String username, boolean isCurrentUser) {
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBackground(Color.WHITE);

        if (isCurrentUser) {
            userPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(1, 1, 1, 1, PRIMARY_COLOR),
                            BorderFactory.createEmptyBorder(8, 8, 8, 8))));
        } else {
            userPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        }

        JLabel userLabel = new JLabel(username + (isCurrentUser ? " (bạn)" : ""));
        userLabel.setFont(NORMAL_FONT);
        if (isCurrentUser) {
            userLabel.setForeground(PRIMARY_COLOR);
            userLabel.setFont(new Font(NORMAL_FONT.getName(), Font.BOLD, NORMAL_FONT.getSize()));
        }

        userLabel.setIcon(avatarIcon);
        userLabel.setIconTextGap(15);

        userPanel.add(userLabel, BorderLayout.WEST);

        JPanel statusPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(SUCCESS_COLOR);
                g2d.fillOval(0, 0, 12, 12);
            }
        };
        statusPanel.setOpaque(false);
        statusPanel.setPreferredSize(new Dimension(12, 12));

        JPanel indicatorContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        indicatorContainer.setOpaque(false);
        indicatorContainer.add(statusPanel);

        userPanel.add(indicatorContainer, BorderLayout.EAST);

        userPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, userPanel.getPreferredSize().height));
        return userPanel;
    }

    private void disconnectFromServer() {
        if (isConnected) {
            client.disconnect();
            isConnected = false;
            cardLayout.show(mainPanel, "login");
            setTitle("Chat Client");
            onlineUsers.clear();
        }
    }

    public void handleServerShutdown() {
        SwingUtilities.invokeLater(() -> {
            if (isConnected) {
                isConnected = false;
                cardLayout.show(mainPanel, "login");
                setTitle("Chat Client");
                JOptionPane.showMessageDialog(ChatClientGUI.this, 
                        "Server đã đóng kết nối!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                onlineUsers.clear();
            }
        });
    }

    private void sendMessage() {
        String message = taMessage.getText().trim();
        if (!message.isEmpty() && isConnected) {
            try {
                long currentTime = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                String timeStamp = sdf.format(new Date(currentTime));
                
                boolean showCenterTime = (currentTime - lastMessageTime) > 5 * 60 * 1000;
                if (showCenterTime) {
                    displayCenterTime(timeStamp);
                }
                
                lastMessageTime = currentTime;
                
                displaySentMessage(message);

                String formattedMessage = getUsername() + ": " + message;
                client.sendMessage(formattedMessage);

                taMessage.setText("");
                taMessage.requestFocus();
            } catch (Exception e) {
                displaySystemMessage("Lỗi khi gửi tin nhắn: " + e.getMessage());
                logger.log(Level.SEVERE, "Error sending message", e);
            }
        }
    }

    public void displaySystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timeStamp = sdf.format(new Date());
            
            String formattedMessage = String.format(
                    "[%s] %s", 
                    timeStamp, 
                    message);
            
            Style styleToUse = systemStyle;
            if (message.contains(" đã tham gia chat!")) {
                styleToUse = joinLeaveStyle;
                String username = message.substring(0, message.indexOf(" đã tham gia chat!"));
                updateUserList(username, true);
            } else if (message.contains(" đã rời chat!")) {
                styleToUse = joinLeaveStyle;
                StyleConstants.setForeground(joinLeaveStyle, LEAVE_COLOR);
                String username = message.substring(0, message.indexOf(" đã rời chat!"));
                updateUserList(username, false);
                StyleConstants.setForeground(joinLeaveStyle, JOIN_COLOR);
            }
            
            try {
                int start = chatDocument.getLength();
                chatDocument.insertString(start, formattedMessage + "\n", styleToUse);
                
                SimpleAttributeSet center = new SimpleAttributeSet();
                StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
                chatDocument.setParagraphAttributes(start, chatDocument.getLength() - start, center, false);
            } catch (BadLocationException e) {
                logger.log(Level.WARNING, "Error displaying system message", e);
                tpChat.setText(tpChat.getText() + formattedMessage + "\n");
            }
            
            tpChat.setCaretPosition(chatDocument.getLength());
        });
    }
    
    public void displaySentMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                String timeStamp = sdf.format(new Date());
                
                int start = chatDocument.getLength();
                chatDocument.insertString(start, "\n", null);
                
                JPanel messagePanel = new JPanel(new BorderLayout(5, 2));
                messagePanel.setBackground(new Color(0, 0, 0, 0));
                
                JPanel bubblePanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(MY_MESSAGE_BG);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                        g2.dispose();
                    }
                };
                bubblePanel.setOpaque(false);
                bubblePanel.setLayout(new BorderLayout());
                
                String wrappedMessage = wrapMessage(message, MAX_CHARS_PER_LINE);
                
                JLabel messageLabel = new JLabel("<html><div style='color: white; padding: 5px 10px 5px 10px;'>" + 
                                            wrappedMessage.replace("\n", "<br>") + 
                                            "<div style='text-align: right; font-size: smaller; margin-top: 4px; color: rgba(255,255,255,0.8);'>" + 
                                            timeStamp + "</div></div></html>");
                messageLabel.setFont(CHAT_FONT);
                bubblePanel.add(messageLabel, BorderLayout.CENTER);
                
                messagePanel.add(bubblePanel, BorderLayout.LINE_END);
                
                Style style = chatDocument.addStyle("MessagePanelStyle", null);
                StyleConstants.setComponent(style, messagePanel);
                
                start = chatDocument.getLength();
                chatDocument.insertString(start, " ", style);
                
                SimpleAttributeSet right = new SimpleAttributeSet();
                StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
                chatDocument.setParagraphAttributes(start, 1, right, false);
                
                chatDocument.insertString(start + 1, "\n", null);
                
                tpChat.setCaretPosition(chatDocument.getLength());
            } catch (BadLocationException e) {
                logger.log(Level.WARNING, "Error displaying sent message", e);
            }
        });
    }
    
    public void displayReceivedMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            long currentTime = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            String timeStamp = sdf.format(new Date(currentTime));
            
            boolean showCenterTime = (currentTime - lastMessageTime) > 5 * 60 * 1000;
            if (showCenterTime) {
                displayCenterTime(timeStamp);
            }
            
            lastMessageTime = currentTime;
            
            String sender;
            String content;
            int colonIndex = message.indexOf(": ");
            
            if (colonIndex > 0) {
                sender = message.substring(0, colonIndex);
                content = message.substring(colonIndex + 2);
            } else {
                sender = "Unknown";
                content = message;
            }
            
            try {
                int start = chatDocument.getLength();
                chatDocument.insertString(start, "\n", null);
                
                JPanel messagePanel = new JPanel(new BorderLayout(5, 2));
                messagePanel.setBackground(new Color(0, 0, 0, 0));
                
                JLabel senderLabel = new JLabel(sender);
                senderLabel.setFont(SMALL_FONT);
                senderLabel.setForeground(SYSTEM_MESSAGE_COLOR);
                messagePanel.add(senderLabel, BorderLayout.NORTH);
                
                JPanel bubblePanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(OTHER_MESSAGE_BG);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                        g2.dispose();
                    }
                };
                bubblePanel.setOpaque(false);
                bubblePanel.setLayout(new BorderLayout());
                
                String wrappedContent = wrapMessage(content, MAX_CHARS_PER_LINE);
                
                JLabel messageLabel = new JLabel("<html><div style='color: black; padding: 5px 10px 5px 10px;'>" + 
                                            wrappedContent.replace("\n", "<br>") + 
                                            "<div style='text-align: right; font-size: smaller; margin-top: 4px; color: rgba(0,0,0,0.6);'>" + 
                                            timeStamp + "</div></div></html>");
                messageLabel.setFont(CHAT_FONT);
                bubblePanel.add(messageLabel, BorderLayout.CENTER);
                
                messagePanel.add(bubblePanel, BorderLayout.LINE_START);
                
                Style style = chatDocument.addStyle("OtherMessagePanelStyle", null);
                StyleConstants.setComponent(style, messagePanel);
                
                start = chatDocument.getLength();
                chatDocument.insertString(start, " ", style);
                
                SimpleAttributeSet left = new SimpleAttributeSet();
                StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);
                chatDocument.setParagraphAttributes(start, 1, left, false);
                
                chatDocument.insertString(start + 1, "\n", null);
            } catch (BadLocationException e) {
                logger.log(Level.WARNING, "Error displaying received message", e);
                try {
                    chatDocument.insertString(chatDocument.getLength(), 
                                            timeStamp + " " + sender + ": " + content + "\n", 
                                            otherMessageStyle);
                } catch (BadLocationException ex) {
                    logger.log(Level.SEVERE, "Cannot insert fallback message", ex);
                }
            }
            
            tpChat.setCaretPosition(chatDocument.getLength());
        });
    }
    
    private String wrapMessage(String message, int charsPerLine) {
        StringBuilder result = new StringBuilder();
        String[] words = message.split(" ");
        int lineLength = 0;
        
        for (String word : words) {
            if (word.length() > charsPerLine) {
                if (lineLength > 0) {
                    result.append("\n");
                    lineLength = 0;
                }
                
                for (int i = 0; i < word.length(); i += charsPerLine) {
                    int end = Math.min(i + charsPerLine, word.length());
                    result.append(word.substring(i, end));
                    if (end < word.length()) {
                        result.append("\n");
                        lineLength = 0;
                    } else {
                        lineLength = end - i;
                    }
                }
                continue;
            }
            
            if (lineLength + word.length() > charsPerLine) {
                result.append("\n");
                lineLength = 0;
            } else if (lineLength > 0) {
                result.append(" ");
                lineLength++;
            }
            
            result.append(word);
            lineLength += word.length();
        }
        
        return result.toString();
    }
    
    public void displayFileMessage(String sender, String filename, String fileType, String fileDetails) {
        SwingUtilities.invokeLater(() -> {
            long currentTime = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            String timeStamp = sdf.format(new Date(currentTime));
            
            boolean showCenterTime = (currentTime - lastMessageTime) > 5 * 60 * 1000;
            if (showCenterTime) {
                displayCenterTime(timeStamp);
            }
            
            lastMessageTime = currentTime;
            
            try {
                int start = chatDocument.getLength();
                chatDocument.insertString(start, "\n", null);
                
                boolean isSentByMe = sender.equals(lblUserInfo.getText());
                
                String fileKey = sender + "_" + filename + "_" + System.currentTimeMillis();
                
                JPanel filePanel = new JPanel(new BorderLayout(5, 2));
                filePanel.setBackground(new Color(0, 0, 0, 0));
                
                if (!isSentByMe) {
                    JLabel senderLabel = new JLabel(sender);
                    senderLabel.setFont(SMALL_FONT);
                    senderLabel.setForeground(SYSTEM_MESSAGE_COLOR);
                    filePanel.add(senderLabel, BorderLayout.NORTH);
                }
                
                JPanel fileContainer = createEnhancedFileContainer(filename, fileType, fileDetails, isSentByMe, fileKey, timeStamp);
                
                if (isSentByMe) {
                    filePanel.add(fileContainer, BorderLayout.LINE_END);
                } else {
                    filePanel.add(fileContainer, BorderLayout.LINE_START);
                }
                
                Style style = chatDocument.addStyle("FileMessageStyle", null);
                StyleConstants.setComponent(style, filePanel);
                
                start = chatDocument.getLength();
                chatDocument.insertString(start, " ", style);
                
                SimpleAttributeSet alignment = new SimpleAttributeSet();
                if (isSentByMe) {
                    StyleConstants.setAlignment(alignment, StyleConstants.ALIGN_RIGHT);
                } else {
                    StyleConstants.setAlignment(alignment, StyleConstants.ALIGN_LEFT);
                }
                chatDocument.setParagraphAttributes(start, 1, alignment, false);
                
                chatDocument.insertString(start + 1, "\n", null);
                
                fileComponentMap.put(fileKey, fileContainer);
            } catch (BadLocationException e) {
                logger.log(Level.WARNING, "Error displaying file message", e);
                String basicMessage = "[" + timeStamp + "] " + sender + " gửi file: " + filename;
                try {
                    chatDocument.insertString(chatDocument.getLength(), basicMessage + "\n", systemStyle);
                } catch (BadLocationException ex) {
                    logger.log(Level.SEVERE, "Cannot insert fallback file message", ex);
                }
            }
            
            tpChat.setCaretPosition(chatDocument.getLength());
        });
    }

    private JPanel createEnhancedFileContainer(String filename, String fileType, String fileDetails, boolean isSentByMe, String fileKey, String timeStamp) {
        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSentByMe ? MY_MESSAGE_BG : OTHER_MESSAGE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        container.setLayout(new BorderLayout(5, 5));
        container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        container.setOpaque(false);
        
        int containerWidth = Math.min(380, tpChat.getWidth() / 2);
        container.setPreferredSize(new Dimension(containerWidth, -1));
        
        JComponent previewComponent = createFilePreviewComponent(filename, fileType, fileKey, fileDetails);
        if (previewComponent != null) {
            container.add(previewComponent, BorderLayout.NORTH);
        }
        
        JPanel infoPanel = new JPanel(new BorderLayout(0, 8));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel("<html><b>" + filename + "</b></html>");
        nameLabel.setFont(new Font(NORMAL_FONT.getFamily(), Font.BOLD, NORMAL_FONT.getSize()));
        nameLabel.setForeground(isSentByMe ? Color.WHITE : Color.BLACK);
        
        JLabel detailsLabel = new JLabel(fileDetails);
        detailsLabel.setFont(SMALL_FONT);
        detailsLabel.setForeground(isSentByMe ? new Color(220, 220, 220) : SYSTEM_MESSAGE_COLOR);
        
        infoPanel.add(nameLabel, BorderLayout.NORTH);
        infoPanel.add(detailsLabel, BorderLayout.CENTER);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionPanel.setOpaque(false);

        JButton viewBtn;
        if (fileType.equals("image")) {
            viewBtn = createFileActionButton("Xem", viewIcon, isSentByMe, SUCCESS_COLOR);
        } else if (fileType.equals("video")) {
            viewBtn = createFileActionButton("Phát", playIcon, isSentByMe, ERROR_COLOR);
        } else if (fileType.equals("audio")) {
            viewBtn = createFileActionButton("Phát", audioIcon, isSentByMe, ACCENT_COLOR);
        } else {
            viewBtn = createFileActionButton("Xem", viewIcon, isSentByMe, SUCCESS_COLOR);
        }
        
        viewBtn.addActionListener(e -> openFileDirectly(filename, fileType, fileKey));
        actionPanel.add(viewBtn);
        
        JButton downloadBtn = createFileActionButton("Tải về", downloadIcon, isSentByMe, ATTACHMENT_COLOR);
        downloadBtn.addActionListener(e -> downloadFile(filename, fileKey));
        actionPanel.add(downloadBtn);
        
        infoPanel.add(actionPanel, BorderLayout.SOUTH);
        
        ImageIcon fileIcon = getFileTypeIcon(fileType);
        Image scaledImage = fileIcon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
        iconLabel.setPreferredSize(new Dimension(48, 48));
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 0));
        contentPanel.setOpaque(false);
        contentPanel.add(iconLabel, BorderLayout.WEST);
        contentPanel.add(infoPanel, BorderLayout.CENTER);
        
        container.add(contentPanel, BorderLayout.CENTER);
        
        JLabel timeLabel = new JLabel(timeStamp);
        timeLabel.setFont(SMALL_FONT);
        timeLabel.setForeground(isSentByMe ? new Color(220, 220, 220, 180) : new Color(100, 100, 100, 180));
        timeLabel.setHorizontalAlignment(isSentByMe ? SwingConstants.RIGHT : SwingConstants.LEFT);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        
        container.add(timeLabel, BorderLayout.SOUTH);
        
        container.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openFileDirectly(filename, fileType, fileKey);
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                container.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                container.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        
        return container;
    }
    
    private JButton createFileActionButton(String text, ImageIcon icon, boolean isSentByMe, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(NORMAL_FONT);
        
        Color buttonColor = isSentByMe ? new Color(255, 255, 255, 180) : baseColor;
        button.setBackground(buttonColor);
        button.setForeground(isSentByMe ? PRIMARY_COLOR : Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (icon != null) {
            button.setIcon(icon);
            button.setIconTextGap(8);
        }
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(buttonColor.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(buttonColor);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(buttonColor.darker().darker());
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(buttonColor.darker());
            }
        });
        
        return button;
    }
    
    private JComponent createFilePreviewComponent(String filename, String fileType, String fileKey, String fileDetails) {
        if (fileType.equals("image")) {
            try {
                File tempFile = new File(tempFilesDir, filename);
                
                if (tempFile.exists()) {
                    try {
                        ImageIcon icon = new ImageIcon(tempFile.getAbsolutePath());
                        int maxWidth = 320;
                        int maxHeight = 220;
                        double scale = getScaleFactor(icon.getIconWidth(), icon.getIconHeight(), maxWidth, maxHeight);
                        
                        Image scaledImage = icon.getImage().getScaledInstance(
                                (int)(icon.getIconWidth() * scale),
                                (int)(icon.getIconHeight() * scale),
                                Image.SCALE_SMOOTH);
                        
                        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                        imageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        return imageLabel;
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error creating image preview", e);
                    }
                } else {
                    JPanel placeholderPanel = new JPanel(new BorderLayout());
                    placeholderPanel.setBackground(new Color(200, 200, 200));
                    placeholderPanel.setPreferredSize(new Dimension(320, 150));
                    
                    JLabel placeholderLabel = new JLabel("Đang tải hình ảnh...");
                    placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    placeholderLabel.setForeground(Color.WHITE);
                    placeholderLabel.setFont(NORMAL_FONT);
                    placeholderPanel.add(placeholderLabel, BorderLayout.CENTER);
                    
                    JProgressBar progressBar = new JProgressBar();
                    progressBar.setIndeterminate(true);
                    progressBar.setPreferredSize(new Dimension(200, 10));
                    progressBar.setBackground(new Color(180, 180, 180));
                    progressBar.setForeground(PRIMARY_COLOR);
                    
                    JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                    spinnerPanel.setOpaque(false);
                    spinnerPanel.add(progressBar);
                    placeholderPanel.add(spinnerPanel, BorderLayout.SOUTH);
                    
                    requestFileAndNotify(filename, tempFile.getAbsolutePath(), fileKey, () -> {
                        updateImagePreview(placeholderPanel, tempFile.getAbsolutePath());
                    });
                    
                    return placeholderPanel;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to create image preview", e);
            }
        } else if (fileType.equals("video")) {
            JPanel videoPanel = new JPanel(new BorderLayout());
            videoPanel.setBackground(Color.BLACK);
            videoPanel.setPreferredSize(new Dimension(320, 180));
            
            JLabel playIconLabel = new JLabel("▶");
            playIconLabel.setFont(new Font("Arial", Font.BOLD, 48));
            playIconLabel.setForeground(new Color(255, 255, 255, 180));
            playIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            JLabel videoInfoLabel = new JLabel(filename);
            videoInfoLabel.setFont(NORMAL_FONT);
            videoInfoLabel.setForeground(Color.WHITE);
            videoInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            videoInfoLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            
            videoPanel.add(playIconLabel, BorderLayout.CENTER);
            videoPanel.add(videoInfoLabel, BorderLayout.SOUTH);
            
            return videoPanel;
        } else if (fileType.equals("audio")) {
            JPanel audioPanel = new JPanel(new BorderLayout());
            audioPanel.setBackground(new Color(240, 240, 240));
            audioPanel.setPreferredSize(new Dimension(320, 60));
            
            JLabel audioLabel = new JLabel(" 🔊  " + filename);
            audioLabel.setFont(NORMAL_FONT);
            audioLabel.setForeground(Color.DARK_GRAY);
            audioLabel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            
            audioPanel.add(audioLabel, BorderLayout.CENTER);
            
            JButton playButton = new JButton("▶");
            playButton.setFont(new Font(NORMAL_FONT.getFamily(), Font.BOLD, 16));
            playButton.setFocusPainted(false);
            playButton.setBackground(SUCCESS_COLOR);
            playButton.setForeground(Color.WHITE);
            playButton.setPreferredSize(new Dimension(50, 50));
            playButton.addActionListener(e -> openFileDirectly(filename, "audio", fileKey));
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setOpaque(false);
            buttonPanel.add(playButton);
            audioPanel.add(buttonPanel, BorderLayout.EAST);
            
            return audioPanel;
        } else {
            JPanel docPanel = new JPanel(new BorderLayout());
            docPanel.setBackground(new Color(245, 245, 245));
            docPanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
            docPanel.setPreferredSize(new Dimension(320, 80));
            
            ImageIcon docIcon = getFileTypeIcon(fileType);
            Image scaledDocImage = docIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            
            JPanel iconTextPanel = new JPanel(new BorderLayout(10, 0));
            iconTextPanel.setOpaque(false);
            iconTextPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JLabel iconLabel = new JLabel(new ImageIcon(scaledDocImage));
            
            JLabel docLabel = new JLabel("<html><b>" + filename + "</b><br><font size='2' color='gray'>" + 
                                   fileDetails + "</font></html>");
            
            iconTextPanel.add(iconLabel, BorderLayout.WEST);
            iconTextPanel.add(docLabel, BorderLayout.CENTER);
            
            docPanel.add(iconTextPanel, BorderLayout.CENTER);
            
            return docPanel;
        }
        return null;
    }
    
    private double getScaleFactor(int width, int height, int maxWidth, int maxHeight) {
        double scaleW = maxWidth / (double)width;
        double scaleH = maxHeight / (double)height;
        return Math.min(1.0, Math.min(scaleW, scaleH));
    }
    
    private void updateImagePreview(JComponent placeholder, String imagePath) {
        SwingUtilities.invokeLater(() -> {
            try {
                ImageIcon icon = new ImageIcon(imagePath);
                
                int maxWidth = 320;
                int maxHeight = 220;
                double scale = getScaleFactor(icon.getIconWidth(), icon.getIconHeight(), maxWidth, maxHeight);
                
                Image scaledImage = icon.getImage().getScaledInstance(
                        (int)(icon.getIconWidth() * scale),
                        (int)(icon.getIconHeight() * scale),
                        Image.SCALE_SMOOTH);
                
                if (placeholder instanceof JPanel) {
                    JPanel panel = (JPanel)placeholder;
                    panel.removeAll();
                    panel.setBackground(null);
                    
                    JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                    imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    panel.add(imageLabel, BorderLayout.CENTER);
                    
                    panel.revalidate();
                    panel.repaint();
                }
                
                tpChat.revalidate();
                tpChat.repaint();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error updating image preview", e);
            }
        });
    }
    
    private void openFileDirectly(String filename, String fileType, String fileKey) {
        try {
            File tempFile = new File(tempFilesDir, filename);
            
            if (tempFile.exists()) {
                openFileWithDefaultApp(tempFile);
            } else {
                JDialog loadingDialog = new JDialog(this, "Đang tải file...", false);
                loadingDialog.setLayout(new BorderLayout(10, 10));
                loadingDialog.setSize(300, 100);
                loadingDialog.setLocationRelativeTo(this);
                
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                JLabel statusLabel = new JLabel("Đang tải " + filename + "...");
                statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
                
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                panel.add(statusLabel, BorderLayout.NORTH);
                panel.add(progressBar, BorderLayout.CENTER);
                
                loadingDialog.add(panel);
                loadingDialog.setVisible(true);
                
                displaySystemMessage("Đang tải " + filename + " để mở...");
                
                requestFileAndNotify(filename, tempFile.getAbsolutePath(), fileKey, () -> {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        try {
                            openFileWithDefaultApp(tempFile);
                        } catch (Exception e) {
                            showErrorMessage("Không thể mở file: " + e.getMessage());
                            logger.log(Level.SEVERE, "Error opening file", e);
                        }
                    });
                });
            }
        } catch (Exception e) {
            showErrorMessage("Lỗi khi mở file: " + e.getMessage());
            logger.log(Level.SEVERE, "Error preparing file for opening", e);
        }
    }
    
    private void downloadFile(String filename, String fileKey) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Lưu file");
            fileChooser.setSelectedFile(new File(filename));
            
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File targetFile = fileChooser.getSelectedFile();
                
                JDialog progressDialog = new JDialog(this, "Đang tải về...", false);
                progressDialog.setLayout(new BorderLayout(10, 10));
                progressDialog.setSize(350, 120);
                progressDialog.setLocationRelativeTo(this);
                
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                progressBar.setString("0%");
                
                JLabel statusLabel = new JLabel("Đang tải " + filename + "...");
                statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
                
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                panel.add(statusLabel, BorderLayout.NORTH);
                panel.add(progressBar, BorderLayout.CENTER);
                
                progressDialog.add(panel);
                progressDialog.setVisible(true);
                
                Runnable updateProgress = () -> {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        displaySystemMessage("Đã tải về file: " + filename);
                    });
                };
                
                requestFileAndNotify(filename, targetFile.getAbsolutePath(), fileKey, updateProgress);
                
                Timer progressTimer = new Timer(500, e -> {
                    Integer progress = fileProgressMap.get(filename);
                    if (progress != null) {
                        progressBar.setValue(progress);
                        progressBar.setString(progress + "%");
                        
                        if (progress >= 100) {
                            ((Timer)e.getSource()).stop();
                            progressDialog.dispose();
                        }
                    }
                });
                progressTimer.start();
            }
        } catch (Exception e) {
            showErrorMessage("Lỗi khi tải file: " + e.getMessage());
            logger.log(Level.SEVERE, "Error downloading file", e);
        }
    }
    
    private ImageIcon getFileTypeIcon(String fileType) {
        switch (fileType) {
            case "audio": return audioIcon;
            case "video": return videoIcon;
            case "image": return imageIcon;
            case "document": return documentIcon;
            default: return fileIcon;
        }
    }
    
    private void openFileWithDefaultApp(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(file);
            } else {
                displaySystemMessage("Không thể mở file tự động. File đã được tải về tại: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showErrorMessage("Không thể mở file: " + e.getMessage());
            logger.log(Level.SEVERE, "Error opening file with default app", e);
        }
    }
    
    public void fileDownloadComplete(String fileName, String savePath) {
        SwingUtilities.invokeLater(() -> {
            displaySystemMessage("Đã tải xuống file: " + fileName + " vào " + savePath);
            
            for (Map.Entry<String, Runnable> entry : fileDownloadCallbacks.entrySet()) {
                if (entry.getKey().startsWith(savePath)) {
                    Runnable callback = entry.getValue();
                    if (callback != null) {
                        callback.run();
                    }
                    fileDownloadCallbacks.remove(entry.getKey());
                    break;
                }
            }
        });
    }
    
    public void requestFileAndNotify(String filename, String savePath, String fileKey, Runnable callback) {
        String callbackKey = savePath + "_" + System.currentTimeMillis();
        
        if (callback != null) {
            fileDownloadCallbacks.put(callbackKey, callback);
        }
        
        client.requestFile(filename, savePath);
    }
    
    public void handleExistingSession(String username) {
        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showConfirmDialog(
                this,
                "Tài khoản \"" + username + "\" đã đăng nhập ở nơi khác.\n" +
                "Bạn có muốn tiếp tục đăng nhập và ngắt kết nối phiên khác không?",
                "Tài khoản đã đăng nhập",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (option == JOptionPane.YES_OPTION) {
                client.forceLogin();
                displaySystemMessage("Bạn đã đăng nhập và ngắt kết nối phiên khác.");
                isConnected = true;
                lblUserInfo.setText(username);
                lblServerInfo.setText("Server: " + tfServerIP.getText().trim());
                lblPortInfo.setText("Port: " + tfPort.getText().trim());
                cardLayout.show(mainPanel, "chat");
                setTitle("Chat Client - " + username);
                updateUserList(username, true);
                requestChatHistory();
                taMessage.requestFocus();
            } else {
                disconnectFromServer();
                displaySystemMessage("Đăng nhập thất bại: Tài khoản đã đăng nhập ở nơi khác.");
                cardLayout.show(mainPanel, "login");
            }
        });
    }

    private void requestChatHistory() {
        if (client != null && client.isConnected()) {
            client.sendMessage("REQUEST_CHAT_HISTORY");
            displaySystemMessage("Đang tải lịch sử chat...");
        }
    }

    public void displayChatHistory(List<String> messages, List<String> authors, List<String> timestamps) {
        SwingUtilities.invokeLater(() -> {
            displaySystemMessage("Đã tải lịch sử chat.");
            
            displaySystemMessage("--- Bắt đầu lịch sử chat ---");
            
            for (int i = 0; i < messages.size(); i++) {
                String author = authors.get(i);
                String message = messages.get(i);
                String timestamp = timestamps.get(i);
                
                if (author.equals(getUsername())) {
                    displayHistoricalSentMessage(message, timestamp);
                } else {
                    displayHistoricalReceivedMessage(author + ": " + message, timestamp);
                }
            }
            
        });
    }

    private void displayHistoricalSentMessage(String message, String timestamp) {
        try {
            int start = chatDocument.getLength();
            chatDocument.insertString(start, "\n", null);
            
            JPanel messagePanel = new JPanel(new BorderLayout(5, 2));
            messagePanel.setBackground(new Color(0, 0, 0, 0));
            
            JPanel bubblePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(MY_MESSAGE_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.dispose();
                }
            };
            bubblePanel.setOpaque(false);
            bubblePanel.setLayout(new BorderLayout());
            
            String wrappedMessage = wrapMessage(message, MAX_CHARS_PER_LINE);
            
            JLabel messageLabel = new JLabel("<html><div style='color: white; padding: 5px 10px 5px 10px;'>" + 
                                        wrappedMessage.replace("\n", "<br>") + 
                                        "<div style='text-align: right; font-size: smaller; margin-top: 4px; color: rgba(255,255,255,0.7);'>" + 
                                        timestamp + " (cũ)</div></div></html>");
            messageLabel.setFont(CHAT_FONT);
            bubblePanel.add(messageLabel, BorderLayout.CENTER);
            
            messagePanel.add(bubblePanel, BorderLayout.LINE_END);
            
            Style style = chatDocument.addStyle("HistoricalMessagePanelStyle", null);
            StyleConstants.setComponent(style, messagePanel);
            
            start = chatDocument.getLength();
            chatDocument.insertString(start, " ", style);
            
            SimpleAttributeSet right = new SimpleAttributeSet();
            StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
            chatDocument.setParagraphAttributes(start, 1, right, false);
            
            chatDocument.insertString(start + 1, "\n", null);
        } catch (BadLocationException e) {
            logger.log(Level.WARNING, "Error displaying historical sent message", e);
        }
    }

    private void displayHistoricalReceivedMessage(String message, String timestamp) {
        String sender;
        String content;
        int colonIndex = message.indexOf(": ");
        
        if (colonIndex > 0) {
            sender = message.substring(0, colonIndex);
            content = message.substring(colonIndex + 2);
        } else {
            sender = "Unknown";
            content = message;
        }
        
        try {
            int start = chatDocument.getLength();
            chatDocument.insertString(start, "\n", null);
            
            JPanel messagePanel = new JPanel(new BorderLayout(5, 2));
            messagePanel.setBackground(new Color(0, 0, 0, 0));
            
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(SMALL_FONT);
            senderLabel.setForeground(SYSTEM_MESSAGE_COLOR);
            messagePanel.add(senderLabel, BorderLayout.NORTH);
            
            JPanel bubblePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(OTHER_MESSAGE_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                    g2.dispose();
                }
            };
            bubblePanel.setOpaque(false);
            bubblePanel.setLayout(new BorderLayout());
            
            String wrappedContent = wrapMessage(content, MAX_CHARS_PER_LINE);
            
            JLabel messageLabel = new JLabel("<html><div style='color: black; padding: 5px 10px 5px 10px;'>" + 
                                      wrappedContent.replace("\n", "<br>") + 
                                      "<div style='text-align: right; font-size: smaller; margin-top: 4px; color: rgba(0,0,0,0.6);'>" + 
                                      timestamp + " (cũ)</div></div></html>");
            messageLabel.setFont(CHAT_FONT);
            bubblePanel.add(messageLabel, BorderLayout.CENTER);
            
            messagePanel.add(bubblePanel, BorderLayout.LINE_START);
            
            Style style = chatDocument.addStyle("HistoricalOtherMessagePanelStyle", null);
            StyleConstants.setComponent(style, messagePanel);
            
            start = chatDocument.getLength();
            chatDocument.insertString(start, " ", style);
            
            SimpleAttributeSet left = new SimpleAttributeSet();
            StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);
            chatDocument.setParagraphAttributes(start, 1, left, false);
            
            chatDocument.insertString(start + 1, "\n", null);
        } catch (BadLocationException e) {
            logger.log(Level.WARNING, "Error displaying historical received message", e);
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SwingUtilities.invokeLater(() -> new ChatClientGUI().setVisible(true));
    }
    
    public void clearUserList() {
        SwingUtilities.invokeLater(() -> {
            onlineUsers.clear();
            updateUserListPanel();
        });
    }
}