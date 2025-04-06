package chatclient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class ChatClientGUI extends JFrame {
    private JTextField tfServerIP, tfPort, tfUsername, tfMessage;
    private JPasswordField pfPassword, pfPasswordConfirm;
    private JTextArea taChat;
    private JButton btnLogin, btnRegister, btnSwitchToRegister, btnSwitchToLogin, btnDisconnect, btnSend;
    private JButton btnSendFile, btnSendImage, btnSendAudio, btnSendVideo;
    private JPanel loginPanel, registerPanel, chatPanel;
    private ChatClient client;
    private boolean isConnected = false;
    private static int clientCounter = 0;
    private static final Logger logger = Logger.getLogger(ChatClientGUI.class.getName());
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JLabel lblUserInfo, lblServerInfo, lblPortInfo;
    private LoginRegisterManager loginManager;
    private FileTransferHandler fileHandler;

    // Colors
    private final Color PRIMARY_COLOR = new Color(33, 150, 243);    // Blue primary
    private final Color ACCENT_COLOR = new Color(255, 87, 34);      // Orange accent
    private final Color SUCCESS_COLOR = new Color(76, 175, 80);     // Green success
    private final Color ERROR_COLOR = new Color(244, 67, 54);       // Red error
    private final Color BACKGROUND_COLOR = new Color(245, 245, 245); // Light gray background
    private final Color TEXT_COLOR = new Color(33, 33, 33);         // Dark text
    private final Color LIGHT_TEXT = new Color(255, 255, 255);      // White text
    private final Color CHAT_BG = new Color(235, 242, 250);        // Light blue chat background
    private final Color MY_MESSAGE_BG = new Color(220, 237, 255);   // Light blue for my messages
    private final Color OTHER_MESSAGE_BG = new Color(235, 235, 235); // Light gray for others' messages

    // Fonts
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 24);
    private final Font SUB_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 12);
    private final Font CHAT_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    
    // Icons
    private ImageIcon sendIcon;
    private ImageIcon fileIcon;
    private ImageIcon imageIcon;
    private ImageIcon audioIcon;
    private ImageIcon videoIcon;
    private ImageIcon loginIcon;
    private ImageIcon registerIcon;
    private ImageIcon disconnectIcon;

    public ChatClientGUI() {
        client = new ChatClient(this);
        loginManager = new LoginRegisterManager();
        fileHandler = new FileTransferHandler(this);
        clientCounter++;
        loadIcons();
        setLookAndFeel();
        initComponents();
        setupLogger();
    }
    
    private void loadIcons() {
        try {
            // Load icons from resources
            sendIcon = new ImageIcon(getClass().getResource("/resources/send_icon.png"));
            fileIcon = new ImageIcon(getClass().getResource("/resources/file_icon.png"));
            imageIcon = new ImageIcon(getClass().getResource("/resources/image_icon.png"));
            audioIcon = new ImageIcon(getClass().getResource("/resources/audio_icon.png"));
            videoIcon = new ImageIcon(getClass().getResource("/resources/video_icon.png"));
            loginIcon = new ImageIcon(getClass().getResource("/resources/login_icon.png"));
            registerIcon = new ImageIcon(getClass().getResource("/resources/register_icon.png"));
            disconnectIcon = new ImageIcon(getClass().getResource("/resources/disconnect_icon.png"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load icons", e);
            // Create placeholder icons if resources not found
            sendIcon = null;
            fileIcon = null;
            imageIcon = null;
            audioIcon = null;
            videoIcon = null;
            loginIcon = null;
            registerIcon = null;
            disconnectIcon = null;
        }
    }
    
    private void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
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
        
        // Customize scrollbar
        UIManager.put("ScrollBar.thumbDarkShadow", PRIMARY_COLOR);
        UIManager.put("ScrollBar.thumbHighlight", PRIMARY_COLOR);
        UIManager.put("ScrollBar.thumbShadow", PRIMARY_COLOR);
        UIManager.put("ScrollBar.thumb", PRIMARY_COLOR);
        UIManager.put("ScrollBar.track", BACKGROUND_COLOR);
        
        // Customize text field
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        
        // Customize password field
        UIManager.put("PasswordField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    private void setupLogger() {
        try {
            Handler fileHandler = new FileHandler("chatclient_" + clientCounter + ".log", true);
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
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
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

    private void initComponents() {
        // Thiết lập cơ bản
        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 700);
        setMinimumSize(new Dimension(700, 600));
        setLocationRelativeTo(null);
        
        // Set application icon
        try {
            ImageIcon appIcon = new ImageIcon(getClass().getResource("/resources/chat_icon.png"));
            setIconImage(appIcon.getImage());
        } catch (Exception e) {
            // No icon set if resource not found
        }

        // Sử dụng CardLayout để chuyển đổi giữa các màn hình
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BACKGROUND_COLOR);
        getContentPane().add(mainPanel);

        // Tạo các panel chính
        loginPanel = createLoginPanel();
        registerPanel = createRegisterPanel();
        chatPanel = createChatPanel();

        // Thêm các panel vào mainPanel với CardLayout
        mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");
        mainPanel.add(chatPanel, "chat");

        // Hiển thị panel đăng nhập đầu tiên
        cardLayout.show(mainPanel, "login");

        // Sự kiện đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (isConnected) {
                    client.disconnect();
                }
            }
        });
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        // Logo & Title
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/resources/chat_logo.png"));
            Image scaledImage = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            JLabel lblLogo = new JLabel(new ImageIcon(scaledImage));
            lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
            headerPanel.add(lblLogo);
            
            // Add some spacing
            headerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        } catch (Exception e) {
            // No logo if resource not found
        }
        
        JLabel lblTitle = new JLabel("Chat Client");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(PRIMARY_COLOR);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);
        
        JLabel lblSubtitle = new JLabel("Đăng nhập để bắt đầu trò chuyện");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblSubtitle.setForeground(TEXT_COLOR);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblSubtitle);
        
        panel.add(headerPanel);
        
        // Form Panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)));
        formPanel.setMaximumSize(new Dimension(450, 350));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Username field
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(NORMAL_FONT);
        formPanel.add(lblUsername);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        tfUsername = new JTextField(20);
        tfUsername.setFont(NORMAL_FONT);
        tfUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formPanel.add(tfUsername);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Password field
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(NORMAL_FONT);
        formPanel.add(lblPassword);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        pfPassword = new JPasswordField(20);
        pfPassword.setFont(NORMAL_FONT);
        pfPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formPanel.add(pfPassword);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Server details
        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.X_AXIS));
        serverPanel.setOpaque(false);
        
        // Server IP
        JPanel ipPanel = new JPanel();
        ipPanel.setLayout(new BoxLayout(ipPanel, BoxLayout.Y_AXIS));
        ipPanel.setOpaque(false);
        
        JLabel lblServerIP = new JLabel("Server IP");
        lblServerIP.setFont(NORMAL_FONT);
        ipPanel.add(lblServerIP);
        ipPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        tfServerIP = new JTextField("localhost");
        tfServerIP.setFont(NORMAL_FONT);
        tfServerIP.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        ipPanel.add(tfServerIP);
        
        // Port
        JPanel portPanel = new JPanel();
        portPanel.setLayout(new BoxLayout(portPanel, BoxLayout.Y_AXIS));
        portPanel.setOpaque(false);
        portPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        
        JLabel lblPort = new JLabel("Port");
        lblPort.setFont(NORMAL_FONT);
        portPanel.add(lblPort);
        portPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        tfPort = new JTextField("12345");
        tfPort.setFont(NORMAL_FONT);
        tfPort.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        portPanel.add(tfPort);
        
        serverPanel.add(ipPanel);
        serverPanel.add(portPanel);
        
        formPanel.add(serverPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        
        // Login button
        btnLogin = createIconButton("Đăng nhập", loginIcon, PRIMARY_COLOR);
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnLogin.addActionListener(e -> {
            // Xử lý đăng nhập
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
                
                // Kiểm tra đăng nhập
                boolean loginValid = loginManager.login(username, password);
                
                if (!loginValid) {
                    showErrorMessage("Thông tin đăng nhập không chính xác!");
                    return;
                }
                
                // Kết nối đến server
                if (client.connect(serverIP, port, username)) {
                    isConnected = true;
                    lblUserInfo.setText("User: " + username);
                    lblServerInfo.setText("Server: " + serverIP);
                    lblPortInfo.setText("Port: " + port);
                    cardLayout.show(mainPanel, "chat");
                    setTitle("Chat Client - " + username);
                    displaySystemMessage("Đã kết nối đến server " + serverIP + " qua port " + port + "!");
                } else {
                    showErrorMessage("Không thể kết nối đến server!");
                }
            } catch (NumberFormatException ex) {
                showErrorMessage("Port không hợp lệ!");
            }
        });
        formPanel.add(btnLogin);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Register link
        JPanel registerLinkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        registerLinkPanel.setOpaque(false);
        
        JLabel lblNoAccount = new JLabel("Chưa có tài khoản? ");
        lblNoAccount.setFont(SMALL_FONT);
        
        btnSwitchToRegister = new JButton("Đăng ký");
        btnSwitchToRegister.setFont(SMALL_FONT);
        btnSwitchToRegister.setBorderPainted(false);
        btnSwitchToRegister.setContentAreaFilled(false);
        btnSwitchToRegister.setForeground(PRIMARY_COLOR);
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

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        // Title
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        JLabel lblTitle = new JLabel("Tạo tài khoản mới");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(PRIMARY_COLOR);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);
        
        JLabel lblSubtitle = new JLabel("Vui lòng nhập thông tin đăng ký");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblSubtitle.setForeground(TEXT_COLOR);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblSubtitle);
        
        panel.add(headerPanel);
        
        // Form Panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)));
        formPanel.setMaximumSize(new Dimension(450, 350));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Username field
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(NORMAL_FONT);
        formPanel.add(lblUsername);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        JTextField tfRegUsername = new JTextField(20);
        tfRegUsername.setFont(NORMAL_FONT);
        tfRegUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formPanel.add(tfRegUsername);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Password field
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(NORMAL_FONT);
        formPanel.add(lblPassword);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        JPasswordField pfRegPassword = new JPasswordField(20);
        pfRegPassword.setFont(NORMAL_FONT);
        pfRegPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formPanel.add(pfRegPassword);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Confirm Password field
        JLabel lblConfirmPassword = new JLabel("Confirm Password");
        lblConfirmPassword.setFont(NORMAL_FONT);
        formPanel.add(lblConfirmPassword);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        pfPasswordConfirm = new JPasswordField(20);
        pfPasswordConfirm.setFont(NORMAL_FONT);
        pfPasswordConfirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formPanel.add(pfPasswordConfirm);
        formPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        
        // Register button
        btnRegister = createIconButton("Đăng ký", registerIcon, ACCENT_COLOR);
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnRegister.addActionListener(e -> {
            // Xử lý đăng ký
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
            
            boolean success = loginManager.register(username, password);
            
            if (success) {
                showSuccessMessage("Đăng ký thành công!");
                
                // Chuyển sang form đăng nhập và điền thông tin
                tfUsername.setText(username);
                pfPassword.setText(password);
                cardLayout.show(mainPanel, "login");
            } else {
                showErrorMessage("Tên người dùng đã tồn tại!");
            }
        });
        formPanel.add(btnRegister);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Login link
        JPanel loginLinkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginLinkPanel.setOpaque(false);
        
        JLabel lblHaveAccount = new JLabel("Đã có tài khoản? ");
        lblHaveAccount.setFont(SMALL_FONT);
        
        btnSwitchToLogin = new JButton("Đăng nhập");
        btnSwitchToLogin.setFont(SMALL_FONT);
        btnSwitchToLogin.setBorderPainted(false);
        btnSwitchToLogin.setContentAreaFilled(false);
        btnSwitchToLogin.setForeground(PRIMARY_COLOR);
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

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BACKGROUND_COLOR);

        // Header panel (user info and disconnect button)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        infoPanel.setOpaque(false);
        
        // User info with avatar
        JPanel userPanel = new JPanel(new BorderLayout(10, 0));
        userPanel.setOpaque(false);
        
        try {
            ImageIcon avatarIcon = new ImageIcon(getClass().getResource("/resources/user_avatar.png"));
            Image scaledAvatar = avatarIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            JLabel lblAvatar = new JLabel(new ImageIcon(scaledAvatar));
            userPanel.add(lblAvatar, BorderLayout.WEST);
        } catch (Exception e) {
            // No avatar if resource not found
        }
        
        lblUserInfo = new JLabel("User: N/A");
        lblUserInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblUserInfo.setForeground(LIGHT_TEXT);
        userPanel.add(lblUserInfo, BorderLayout.CENTER);
        
        infoPanel.add(userPanel);
        
        // Server info
        lblServerInfo = new JLabel("Server: N/A");
        lblServerInfo.setFont(NORMAL_FONT);
        lblServerInfo.setForeground(LIGHT_TEXT);
        infoPanel.add(lblServerInfo);
        
        // Port info
        lblPortInfo = new JLabel("Port: N/A");
        lblPortInfo.setFont(NORMAL_FONT);
        lblPortInfo.setForeground(LIGHT_TEXT);
        infoPanel.add(lblPortInfo);
        
        headerPanel.add(infoPanel, BorderLayout.WEST);
        
        // Disconnect button
        btnDisconnect = createIconButton("Đăng xuất", disconnectIcon, ERROR_COLOR);
        btnDisconnect.setFont(SMALL_FONT);
        btnDisconnect.addActionListener(e -> {
            disconnectFromServer();
        });
        
        headerPanel.add(btnDisconnect, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Chat display area
        JPanel chatArea = new JPanel(new BorderLayout());
        chatArea.setBackground(CHAT_BG);
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        taChat = new JTextArea();
        taChat.setEditable(false);
        taChat.setLineWrap(true);
        taChat.setWrapStyleWord(true);
        taChat.setFont(CHAT_FONT);
        taChat.setBackground(Color.WHITE);
        taChat.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(taChat);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatArea.add(scrollPane, BorderLayout.CENTER);
        
        panel.add(chatArea, BorderLayout.CENTER);

        // Input area (message and send button)
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        
        // File buttons panel
        JPanel fileButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        fileButtonsPanel.setOpaque(false);
        
        btnSendFile = new JButton();
        styleIconButton(btnSendFile, fileIcon, "Gửi file");
        btnSendFile.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("file");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        btnSendImage = new JButton();
        styleIconButton(btnSendImage, imageIcon, "Gửi ảnh");
        btnSendImage.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("image");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        btnSendAudio = new JButton();
        styleIconButton(btnSendAudio, audioIcon, "Gửi audio");
        btnSendAudio.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("audio");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        btnSendVideo = new JButton();
        styleIconButton(btnSendVideo, videoIcon, "Gửi video");
        btnSendVideo.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("video");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        fileButtonsPanel.add(btnSendFile);
        fileButtonsPanel.add(btnSendImage);
        fileButtonsPanel.add(btnSendAudio);
        fileButtonsPanel.add(btnSendVideo);
        
        inputPanel.add(fileButtonsPanel, BorderLayout.NORTH);
        
        // Message input field and send button
        JPanel messagePanel = new JPanel(new BorderLayout(10, 0));
        messagePanel.setOpaque(false);
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        tfMessage = new JTextField();
        tfMessage.setFont(NORMAL_FONT);
        tfMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        tfMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        
        messagePanel.add(tfMessage, BorderLayout.CENTER);
        
        btnSend = new JButton();
        styleIconButton(btnSend, sendIcon, "Gửi");
        btnSend.setBackground(PRIMARY_COLOR);
        btnSend.setFont(BUTTON_FONT);
        btnSend.setForeground(LIGHT_TEXT);
        btnSend.addActionListener(e -> {
            sendMessage();
        });
        
        messagePanel.add(btnSend, BorderLayout.EAST);
        
        inputPanel.add(messagePanel, BorderLayout.CENTER);
        
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    private void styleIconButton(JButton button, ImageIcon icon, String tooltip) {
        button.setToolTipText(tooltip);
        if (icon != null) {
            button.setIcon(icon);
        } else {
            button.setText(tooltip);
        }
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(36, 36));
    }

    private void disconnectFromServer() {
        if (isConnected) {
            client.disconnect();
            isConnected = false;
            cardLayout.show(mainPanel, "login");
            setTitle("Chat Client");
            displaySystemMessage("Đã ngắt kết nối từ server!");
        }
    }

    public void handleServerShutdown() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    isConnected = false;
                    cardLayout.show(mainPanel, "login");
                    setTitle("Chat Client");
                    JOptionPane.showMessageDialog(ChatClientGUI.this, 
                            "Server đã đóng kết nối!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
    }

    private void sendMessage() {
        String message = tfMessage.getText().trim();
        if (!message.isEmpty() && isConnected) {
            client.sendMessage(message);
            tfMessage.setText("");
        }
    }

    // Hiển thị tin nhắn hệ thống
    public void displaySystemMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String timeStamp = sdf.format(new Date());
                
                String formattedMessage = String.format(
                        "[%s] %s\n", 
                        timeStamp, 
                        message);
                
                // Style system messages
                taChat.append(formattedMessage);
                
                // Cuộn xuống cuối
                taChat.setCaretPosition(taChat.getDocument().getLength());
            }
        });
    }
    
    // Hiển thị tin nhắn đã gửi
    public void displaySentMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String timeStamp = sdf.format(new Date());
                
                String formattedMessage = String.format(
                        "[%s] Bạn: %s\n", 
                        timeStamp, 
                        message);
                
                // Style your messages
                taChat.append(formattedMessage);
                
                // Cuộn xuống cuối
                taChat.setCaretPosition(taChat.getDocument().getLength());
            }
        });
    }
    
    // Hiển thị tin nhắn nhận được
    public void displayReceivedMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String timeStamp = sdf.format(new Date());
                
                String formattedMessage = String.format(
                        "[%s] %s\n", 
                        timeStamp, 
                        message);
                
                // Style received messages
                taChat.append(formattedMessage);
                
                // Cuộn xuống cuối
                taChat.setCaretPosition(taChat.getDocument().getLength());
            }
        });
    }

    public static void main(String[] args) {
        try {
            // Set the System look and feel
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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClientGUI().setVisible(true);
            }
        });
    }
}