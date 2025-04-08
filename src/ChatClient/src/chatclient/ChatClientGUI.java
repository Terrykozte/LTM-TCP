package chatclient;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.*;
import java.util.regex.Pattern;

public class ChatClientGUI extends JFrame {
    private JTextField tfServerIP, tfPort, tfMessage;
    private JTextField tfUsername; // Không dùng label trên input
    private JPasswordField pfPassword, pfPasswordConfirm;
    private JTextPane tpChat; // Sử dụng JTextPane thay vì JTextArea để hỗ trợ định dạng văn bản
    private JButton btnLogin, btnRegister, btnSwitchToRegister, btnSwitchToLogin, btnDisconnect, btnSend;
    private JButton btnAttachment; // Gộp các nút gửi file thành 1
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
    
    // Document style
    private StyledDocument chatDocument;
    private Style systemStyle, myMessageStyle, otherMessageStyle, joinLeaveStyle;

    // Colors
    private final Color PRIMARY_COLOR = new Color(25, 118, 210);    // Blue primary
    private final Color PRIMARY_DARK_COLOR = new Color(13, 71, 161); // Darker primary
    private final Color ACCENT_COLOR = new Color(255, 87, 34);      // Orange accent
    private final Color ACCENT_DARK_COLOR = new Color(230, 74, 25); // Darker accent
    private final Color SUCCESS_COLOR = new Color(76, 175, 80);     // Green success
    private final Color WARNING_COLOR = new Color(255, 152, 0);     // Orange warning
    private final Color ERROR_COLOR = new Color(244, 67, 54);       // Red error
    private final Color BACKGROUND_COLOR = new Color(245, 245, 245); // Light gray background
    private final Color TEXT_COLOR = new Color(33, 33, 33);         // Dark text
    private final Color LIGHT_TEXT = new Color(255, 255, 255);      // White text
    private final Color CHAT_BG = new Color(235, 242, 250);         // Light blue chat background
    private final Color MY_MESSAGE_BG = new Color(220, 237, 255);   // Light blue for my messages
    private final Color OTHER_MESSAGE_BG = new Color(235, 235, 235); // Light gray for others' messages
    private final Color SYSTEM_MESSAGE_COLOR = new Color(158, 158, 158); // Gray for system messages
    private final Color JOIN_COLOR = new Color(76, 175, 80);         // Green for join messages
    private final Color LEAVE_COLOR = new Color(239, 83, 80);        // Red for leave messages
    private final Color ATTACHMENT_COLOR = new Color(33, 150, 243);  // Blue for attachment button

    // Fonts
    private final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 26);
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
    private ImageIcon appIcon;
    private ImageIcon logoIcon;
    private ImageIcon avatarIcon;
    private ImageIcon attachmentIcon;
    private ImageIcon userListIcon;
    private ImageIcon showPasswordIcon;
    private ImageIcon hidePasswordIcon;

    // Password pattern - yêu cầu có kí tự đặc biệt, chữ hoa, chữ thường và số
    private final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()\\-+=])(?=\\S+$).{8,}$");

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
            sendIcon = createScaledIcon("/resources/send_icon.png", 24, 24);
            fileIcon = createScaledIcon("/resources/file_icon.png", 24, 24);
            imageIcon = createScaledIcon("/resources/image_icon.png", 24, 24);
            audioIcon = createScaledIcon("/resources/audio_icon.png", 24, 24);
            videoIcon = createScaledIcon("/resources/video_icon.png", 24, 24);
            loginIcon = createScaledIcon("/resources/login_icon.png", 20, 20);
            registerIcon = createScaledIcon("/resources/register_icon.png", 20, 20);
            disconnectIcon = createScaledIcon("/resources/disconnect_icon.png", 20, 20);
            appIcon = createScaledIcon("/resources/chat_icon.png", 32, 32);
            logoIcon = createScaledIcon("/resources/chat_logo.png", 120, 120);
            avatarIcon = createScaledIcon("/resources/user_avatar.png", 32, 32);
            attachmentIcon = createScaledIcon("/resources/attachment_icon.png", 24, 24);
            userListIcon = createScaledIcon("/resources/user_list_icon.png", 24, 24);
            showPasswordIcon = createScaledIcon("/resources/show_password.png", 20, 20);
            hidePasswordIcon = createScaledIcon("/resources/hide_password.png", 20, 20);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load icons", e);
            // Placeholder icons will be handled in the UI methods
            createPlaceholderIcons();
        }
    }
    
    private void createPlaceholderIcons() {
        // Create simple placeholder icons if resources not found
        showPasswordIcon = new ImageIcon(createEyeIcon(20, 20, true));
        hidePasswordIcon = new ImageIcon(createEyeIcon(20, 20, false));
    }
    
    private Image createEyeIcon(int width, int height, boolean open) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw eye
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawOval(2, 5, width - 4, height - 10);
        
        if (open) {
            // Draw pupil
            g2d.fillOval(width/2 - 2, height/2 - 2, 4, 4);
        } else {
            // Draw slash for closed eye
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(4, height/2, width - 4, height/2);
        }
        
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
            // Try to set Nimbus look and feel for better appearance
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    
                    // Customize Nimbus colors
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
                // Fallback to system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not set look and feel", ex);
            }
        }
    }

    private void setupLogger() {
        try {
            // Create logs directory if it doesn't exist
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
        
        // Rounded corners for the button
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        
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
    
    // Tạo styled text field với floating label
    private JPanel createStyledTextField(String placeholder, JTextField textField) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel label = new JLabel(placeholder);
        label.setFont(NORMAL_FONT);
        label.setForeground(PRIMARY_COLOR);
        
        // Styling the text field
        textField.setFont(NORMAL_FONT);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        textField.setOpaque(false);
        
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(textField);
        
        return panel;
    }
    
    // Tạo styled password field với floating label và nút show/hide
    private JPanel createStyledPasswordField(String placeholder, JPasswordField passwordField, 
                                            JToggleButton toggleButton) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel label = new JLabel(placeholder);
        label.setFont(NORMAL_FONT);
        label.setForeground(PRIMARY_COLOR);
        
        // Password field with show/hide button
        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.setOpaque(false);
        
        // Styling the password field
        passwordField.setFont(NORMAL_FONT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        passwordField.setOpaque(false);
        
        // Styling the toggle button
        toggleButton.setIcon(hidePasswordIcon);
        toggleButton.setSelectedIcon(showPasswordIcon);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.setToolTipText("Hiển thị/Ẩn mật khẩu");
        
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected()) {
                passwordField.setEchoChar((char) 0); // Show password
            } else {
                passwordField.setEchoChar('•'); // Hide password
            }
        });
        
        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(toggleButton, BorderLayout.EAST);
        
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(passwordPanel);
        
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
        // Thiết lập cơ bản
        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(950, 700);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);
        
        // Cải thiện sự kiện đóng cửa sổ để xác nhận trước khi thoát
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }
        });
        
        // Set application icon
        if (appIcon != null) {
            setIconImage(appIcon.getImage());
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
        panel.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        // Logo & Title
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        if (logoIcon != null) {
            JLabel lblLogo = new JLabel(logoIcon);
            lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
            headerPanel.add(lblLogo);
            
            // Add some spacing
            headerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        }
        
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
        
        // Form Panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));
        formPanel.setMaximumSize(new Dimension(450, 400));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Tạo các trường nhập với floating label
        tfUsername = new JTextField(20);
        JPanel usernamePanel = createStyledTextField("Tên đăng nhập", tfUsername);
        formPanel.add(usernamePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Password field with show/hide button
        pfPassword = new JPasswordField(20);
        btnShowPassword = new JToggleButton();
        JPanel passwordPanel = createStyledPasswordField("Mật khẩu", pfPassword, btnShowPassword);
        formPanel.add(passwordPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Server details
        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.X_AXIS));
        serverPanel.setOpaque(false);
        
        // Server IP
        tfServerIP = new JTextField("localhost", 20);
        JPanel serverIPPanel = createStyledTextField("Địa chỉ server", tfServerIP);
        serverIPPanel.setPreferredSize(new Dimension(0, 70));
        serverIPPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        // Port
        tfPort = new JTextField("12345", 20);
        JPanel portPanel = createStyledTextField("Cổng", tfPort);
        portPanel.setPreferredSize(new Dimension(0, 70));
        portPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        portPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        
        serverPanel.add(serverIPPanel);
        serverPanel.add(portPanel);
        
        formPanel.add(serverPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        
        // Login button
        btnLogin = createIconButton("ĐĂNG NHẬP", loginIcon, PRIMARY_COLOR);
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLogin.addActionListener(e -> handleLogin());
        
        // Allow pressing Enter in username/password field to login
        ActionListener loginAction = e -> handleLogin();
        tfUsername.addActionListener(loginAction);
        pfPassword.addActionListener(loginAction);
        tfServerIP.addActionListener(loginAction);
        tfPort.addActionListener(loginAction);
        
        formPanel.add(btnLogin);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Register link
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
            // Clear register fields before showing register panel
            cardLayout.show(mainPanel, "register");
        });
        
        registerLinkPanel.add(lblNoAccount);
        registerLinkPanel.add(btnSwitchToRegister);
        
        formPanel.add(registerLinkPanel);
        
        panel.add(formPanel);
        
        return panel;
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
            
            // Kiểm tra đăng nhập
            boolean loginValid = loginManager.login(username, password);
            
            if (!loginValid) {
                showErrorMessage("Thông tin đăng nhập không chính xác!");
                return;
            }
            
            // Hiển thị thông báo đang kết nối
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            btnLogin.setEnabled(false);
            btnLogin.setText("Đang kết nối...");
            
            // Sử dụng SwingWorker để không chặn EDT
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    // Kết nối đến server
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
                            lblOnlineUsers.setText("1 người dùng trực tuyến");
                            cardLayout.show(mainPanel, "chat");
                            setTitle("Chat Client - " + username);
                            updateUserList(username, true);
                            displaySystemMessage("Đã kết nối đến server " + serverIP + " qua port " + port + "!");
                            tfMessage.requestFocus();
                        } else {
                            showErrorMessage("Không thể kết nối đến server!");
                        }
                    } catch (Exception ex) {
                        showErrorMessage("Lỗi kết nối: " + ex.getMessage());
                        logger.log(Level.SEVERE, "Connection error", ex);
                    } finally {
                        // Khôi phục giao diện
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
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        // Title
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
        
        // Form Panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(25, 40, 25, 40)));
        formPanel.setMaximumSize(new Dimension(450, 450));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Username field
        JTextField tfRegUsername = new JTextField(20);
        JPanel usernamePanel = createStyledTextField("Tên đăng nhập", tfRegUsername);
        formPanel.add(usernamePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Password field with show/hide button and strength indicator
        JPasswordField pfRegPassword = new JPasswordField(20);
        btnShowPasswordConfirm = new JToggleButton();
        JPanel passwordPanel = createStyledPasswordField("Mật khẩu", pfRegPassword, btnShowPasswordConfirm);
        formPanel.add(passwordPanel);
        
        // Password strength indicator
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
        
        // Password requirements
        JLabel passwordReqLabel = new JLabel("<html>Mật khẩu phải có ít nhất 8 kí tự, bao gồm: chữ hoa, chữ thường, số và kí tự đặc biệt (!@#$%^&*()-+=)</html>");
        passwordReqLabel.setFont(SMALL_FONT);
        passwordReqLabel.setForeground(SYSTEM_MESSAGE_COLOR);
        passwordReqLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));
        formPanel.add(passwordReqLabel);
        
        // Password strength checker
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
        
        // Confirm Password field with show/hide button
        pfPasswordConfirm = new JPasswordField(20);
        JToggleButton btnShowConfirmPassword = new JToggleButton();
        JPanel confirmPasswordPanel = createStyledPasswordField("Xác nhận mật khẩu", pfPasswordConfirm, btnShowConfirmPassword);
        formPanel.add(confirmPasswordPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        
        // Register button
        btnRegister = createIconButton("ĐĂNG KÝ", registerIcon, ACCENT_COLOR);
        btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 16));
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
            
            if (username.length() < 3) {
                showErrorMessage("Tên người dùng phải có ít nhất 3 ký tự!");
                return;
            }
            
            if (password.length() < 8) {
                showErrorMessage("Mật khẩu phải có ít nhất 8 ký tự!");
                return;
            }
            
            // Kiểm tra độ mạnh mật khẩu
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                showErrorMessage("Mật khẩu không đủ mạnh! Mật khẩu phải có ít nhất 8 kí tự, bao gồm: chữ hoa, chữ thường, số và kí tự đặc biệt.");
                return;
            }
            
            // Hiển thị thông báo đang xử lý
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            btnRegister.setEnabled(false);
            btnRegister.setText("Đang xử lý...");
            
            // Sử dụng SwingWorker để không chặn EDT
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
                            
                            // Chuyển sang form đăng nhập và điền thông tin
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
                        // Khôi phục giao diện
                        setCursor(Cursor.getDefaultCursor());
                        btnRegister.setEnabled(true);
                        btnRegister.setText("ĐĂNG KÝ");
                    }
                }
            };
            
            worker.execute();
        });
        
        // Allow pressing Enter to register
        ActionListener registerAction = e -> btnRegister.doClick();
        tfRegUsername.addActionListener(registerAction);
        pfRegPassword.addActionListener(registerAction);
        pfPasswordConfirm.addActionListener(registerAction);
        
        formPanel.add(btnRegister);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Login link
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
    
    // Kiểm tra và hiển thị độ mạnh mật khẩu
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
        
        // Độ dài
        if (password.length() >= 8) score += 20;
        else if (password.length() >= 6) score += 10;
        
        // Có chữ thường
        if (password.matches(".*[a-z].*")) score += 10;
        
        // Có chữ hoa
        if (password.matches(".*[A-Z].*")) score += 15;
        
        // Có số
        if (password.matches(".*[0-9].*")) score += 15;
        
        // Có ký tự đặc biệt
        if (password.matches(".*[!@#$%^&*()\\-+=].*")) score += 20;
        
        // Có cả chữ, số và ký tự đặc biệt
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

        // Header panel (user info and disconnect button)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        infoPanel.setOpaque(false);
        
        // User info with avatar
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userPanel.setOpaque(false);
        
        if (avatarIcon != null) {
            JLabel lblAvatar = new JLabel(avatarIcon);
            userPanel.add(lblAvatar);
        }
        
        lblUserInfo = new JLabel("Username");
        lblUserInfo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblUserInfo.setForeground(LIGHT_TEXT);
        userPanel.add(lblUserInfo);
        
        infoPanel.add(userPanel);
        
        // Server info
        JPanel serverDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        serverDetailsPanel.setOpaque(false);
        
        lblServerInfo = new JLabel("Server: N/A");
        lblServerInfo.setFont(NORMAL_FONT);
        lblServerInfo.setForeground(LIGHT_TEXT);
        serverDetailsPanel.add(lblServerInfo);
        
        // Port info
        lblPortInfo = new JLabel("Port: N/A");
        lblPortInfo.setFont(NORMAL_FONT);
        lblPortInfo.setForeground(LIGHT_TEXT);
        serverDetailsPanel.add(lblPortInfo);
        
        // Online users info
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
                lblOnlineUsers.setText("<html><u>" + lblOnlineUsers.getText() + "</u></html>");
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
        
        // Disconnect button
        btnDisconnect = createIconButton("Đăng xuất", disconnectIcon, ERROR_COLOR);
        btnDisconnect.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDisconnect.addActionListener(e -> {
            // Confirm before disconnecting
            if (showConfirmDialog(
                    "Bạn có chắc chắn muốn đăng xuất?", 
                    "Xác nhận đăng xuất")) {
                disconnectFromServer();
            }
        });
        
        headerPanel.add(btnDisconnect, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Main chat area with fixed layout - khung chat và khung người dùng có kích cỡ cố định
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(BACKGROUND_COLOR);
        
        // Chat display area - fixed size
        JPanel chatArea = new JPanel(new BorderLayout());
        chatArea.setBackground(CHAT_BG);
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatArea.setPreferredSize(new Dimension(700, 0)); // Chiều rộng cố định
        
        // Sử dụng JTextPane thay vì JTextArea để hỗ trợ định dạng văn bản
        tpChat = new JTextPane();
        tpChat.setEditable(false);
        tpChat.setBackground(Color.WHITE);
        tpChat.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Thiết lập styles cho chat
        chatDocument = tpChat.getStyledDocument();
        
        // System message style (gray)
        systemStyle = tpChat.addStyle("System", null);
        StyleConstants.setForeground(systemStyle, SYSTEM_MESSAGE_COLOR);
        StyleConstants.setFontFamily(systemStyle, "Segoe UI");
        StyleConstants.setFontSize(systemStyle, 13);
        StyleConstants.setAlignment(systemStyle, StyleConstants.ALIGN_CENTER);
        StyleConstants.setItalic(systemStyle, true);
        
        // Join/Leave message style
        joinLeaveStyle = tpChat.addStyle("JoinLeave", null);
        StyleConstants.setForeground(joinLeaveStyle, JOIN_COLOR);
        StyleConstants.setFontFamily(joinLeaveStyle, "Segoe UI");
        StyleConstants.setFontSize(joinLeaveStyle, 13);
        StyleConstants.setAlignment(joinLeaveStyle, StyleConstants.ALIGN_CENTER);
        StyleConstants.setItalic(joinLeaveStyle, true);
        
        // My message style (blue)
        myMessageStyle = tpChat.addStyle("Me", null);
        StyleConstants.setForeground(myMessageStyle, PRIMARY_DARK_COLOR);
        StyleConstants.setFontFamily(myMessageStyle, "Segoe UI");
        StyleConstants.setFontSize(myMessageStyle, 14);
        StyleConstants.setBold(myMessageStyle, true);
        StyleConstants.setAlignment(myMessageStyle, StyleConstants.ALIGN_RIGHT);
        
        // Other message style (dark text)
        otherMessageStyle = tpChat.addStyle("Other", null);
        StyleConstants.setForeground(otherMessageStyle, TEXT_COLOR);
        StyleConstants.setFontFamily(otherMessageStyle, "Segoe UI");
        StyleConstants.setFontSize(otherMessageStyle, 14);
        StyleConstants.setAlignment(otherMessageStyle, StyleConstants.ALIGN_LEFT);
        
        JScrollPane chatScrollPane = new JScrollPane(tpChat);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatArea.add(chatScrollPane, BorderLayout.CENTER);
        
        // User list panel - fixed size
        JPanel usersContainerPanel = new JPanel(new BorderLayout());
        usersContainerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
        usersContainerPanel.setBackground(BACKGROUND_COLOR);
        usersContainerPanel.setPreferredSize(new Dimension(250, 0));  // Chiều rộng cố định
        
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
        userListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        usersContainerPanel.add(userListScrollPane, BorderLayout.CENTER);
        
        // Add chat area and user list to main content
        mainContentPanel.add(chatArea, BorderLayout.CENTER);
        mainContentPanel.add(usersContainerPanel, BorderLayout.EAST);
        
        panel.add(mainContentPanel, BorderLayout.CENTER);

        // Input area (message and send button)
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        
        // Message input field and send button
        JPanel messagePanel = new JPanel(new BorderLayout(10, 0));
        messagePanel.setOpaque(false);
        
        // Multi-line message input field
        tfMessage = new JTextField();
        tfMessage.setFont(NORMAL_FONT);
        tfMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        
        // Xử lý Enter để gửi tin nhắn, Shift+Enter để xuống dòng
        tfMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        // Shift+Enter để xuống dòng
                        tfMessage.setText(tfMessage.getText() + "\n");
                    } else {
                        // Enter để gửi tin nhắn
                        sendMessage();
                        e.consume(); // Ngăn không cho Enter thêm dòng mới
                    }
                }
            }
        });
        
        messagePanel.add(tfMessage, BorderLayout.CENTER);
        
        // Attachment button with better styling
        btnAttachment = new JButton("Đính kèm");
        btnAttachment.setIcon(attachmentIcon);
        btnAttachment.setFont(BUTTON_FONT);
        btnAttachment.setForeground(LIGHT_TEXT);
        btnAttachment.setBackground(ATTACHMENT_COLOR);
        btnAttachment.setToolTipText("Gửi file đính kèm");
        btnAttachment.setFocusPainted(false);
        btnAttachment.setBorderPainted(false);
        btnAttachment.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect to attachment button
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
        
        // Create popup menu for attachment options
        JPopupMenu attachmentMenu = new JPopupMenu();
        
        // Customize menu appearance
        attachmentMenu.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        
        JMenuItem imageMenuItem = new JMenuItem("Gửi ảnh", imageIcon);
        imageMenuItem.setFont(NORMAL_FONT);
        
        JMenuItem fileMenuItem = new JMenuItem("Gửi file", fileIcon);
        fileMenuItem.setFont(NORMAL_FONT);
        
        JMenuItem audioMenuItem = new JMenuItem("Gửi audio", audioIcon);
        audioMenuItem.setFont(NORMAL_FONT);
        
        JMenuItem videoMenuItem = new JMenuItem("Gửi video", videoIcon);
        videoMenuItem.setFont(NORMAL_FONT);
        
        // Add action listeners
        imageMenuItem.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("image");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        fileMenuItem.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("file");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        audioMenuItem.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("audio");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        videoMenuItem.addActionListener(e -> {
            File selectedFile = fileHandler.selectFile("video");
            if (selectedFile != null) {
                client.sendFile(selectedFile);
            }
        });
        
        // Add menu items to popup
        attachmentMenu.add(imageMenuItem);
        attachmentMenu.add(fileMenuItem);
        attachmentMenu.add(audioMenuItem);
        attachmentMenu.add(videoMenuItem);
        
        // Show popup when attachment button clicked
        btnAttachment.addActionListener(e -> {
            attachmentMenu.show(btnAttachment, 0, btnAttachment.getHeight());
        });
        
        // Send button
        btnSend = new JButton("Gửi");
        btnSend.setIcon(sendIcon);
        btnSend.setBackground(PRIMARY_COLOR);
        btnSend.setForeground(LIGHT_TEXT);
        btnSend.setFont(BUTTON_FONT);
        btnSend.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btnSend.setFocusPainted(false);
        btnSend.setToolTipText("Gửi tin nhắn (Enter)");
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.addActionListener(e -> {
            sendMessage();
        });
        
        // Add hover effect to send button
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
        
        // Panel with attachment and send buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(btnAttachment);
        buttonsPanel.add(btnSend);
        
        messagePanel.add(buttonsPanel, BorderLayout.EAST);
        
        // Message tips panel
        JPanel tipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tipsPanel.setOpaque(false);
        
        JLabel tipsLabel = new JLabel("Nhấn Enter để gửi, Shift+Enter để xuống dòng");
        tipsLabel.setFont(SMALL_FONT);
        tipsLabel.setForeground(SYSTEM_MESSAGE_COLOR);
        tipsPanel.add(tipsLabel);
        
        // Add message panel and tips to input panel
        inputPanel.add(messagePanel, BorderLayout.CENTER);
        inputPanel.add(tipsPanel, BorderLayout.SOUTH);
        
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    private void showOnlineUsersList() {
        // Create a dialog to display online users
        JDialog userDialog = new JDialog(this, "Người dùng trực tuyến", true);
        userDialog.setSize(300, 400);
        userDialog.setLocationRelativeTo(this);
        userDialog.setLayout(new BorderLayout());
        
        // Create panel to display users
        JPanel usersPanel = new JPanel();
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setBackground(Color.WHITE);
        usersPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Add header
        JLabel headerLabel = new JLabel("NGƯỜI DÙNG TRỰC TUYẾN (" + onlineUsers.size() + ")");
        headerLabel.setFont(SUB_HEADER_FONT);
        headerLabel.setForeground(PRIMARY_COLOR);
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        usersPanel.add(headerLabel);
        
        // Add each user
        for (String user : onlineUsers) {
            JPanel userPanel = new JPanel(new BorderLayout());
            userPanel.setBackground(Color.WHITE);
            userPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                    BorderFactory.createEmptyBorder(10, 5, 10, 5)));
            
            JLabel userLabel = new JLabel(user);
            userLabel.setFont(NORMAL_FONT);
            
            // Add avatar
            if (avatarIcon != null) {
                userLabel.setIcon(avatarIcon);
                userLabel.setIconTextGap(10);
            }
            
            userPanel.add(userLabel, BorderLayout.WEST);
            
            // Add online indicator
            JPanel statusPanel = new JPanel();
            statusPanel.setOpaque(false);
            statusPanel.setPreferredSize(new Dimension(15, 15));
            statusPanel.setBackground(SUCCESS_COLOR);
            statusPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
            
            userPanel.add(statusPanel, BorderLayout.EAST);
            
            // Make the panel full width
            userPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, userPanel.getPreferredSize().height));
            usersPanel.add(userPanel);
        }
        
        // Add scroll pane
        JScrollPane scrollPane = new JScrollPane(usersPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        userDialog.add(scrollPane, BorderLayout.CENTER);
        
        // Add close button
        JButton closeButton = createStyledButton("Đóng", PRIMARY_COLOR);
        closeButton.addActionListener(e -> userDialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.add(closeButton);
        
        userDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Show dialog
        userDialog.setVisible(true);
    }
    
    // Update user list when someone joins or leaves
    public void updateUserList(String username, boolean isJoining) {
        if (isJoining) {
            if (!onlineUsers.contains(username)) {
                onlineUsers.add(username);
            }
        } else {
            onlineUsers.remove(username);
        }
        
        // Update online users label
        lblOnlineUsers.setText(onlineUsers.size() + " người dùng trực tuyến");
        
        // Update user list panel
        updateUserListPanel();
    }
    
    private void updateUserListPanel() {
        // Clear the panel
        userListPanel.removeAll();
        
        JLabel lblUsersHeader = new JLabel("NGƯỜI DÙNG TRỰC TUYẾN (" + onlineUsers.size() + ")");
        lblUsersHeader.setFont(SUB_HEADER_FONT);
        lblUsersHeader.setForeground(PRIMARY_COLOR);
        lblUsersHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        lblUsersHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        userListPanel.add(lblUsersHeader);
        userListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Add each user
        for (String user : onlineUsers) {
            JPanel userPanel = new JPanel(new BorderLayout());
            userPanel.setBackground(Color.WHITE);
            userPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                    BorderFactory.createEmptyBorder(10, 5, 10, 5)));
            
            JLabel userLabel = new JLabel(user);
            userLabel.setFont(NORMAL_FONT);
            
            // Add avatar
            if (avatarIcon != null) {
                userLabel.setIcon(avatarIcon);
                userLabel.setIconTextGap(10);
            }
            
            userPanel.add(userLabel, BorderLayout.WEST);
            
            // Add online indicator
            JPanel statusPanel = new JPanel();
            statusPanel.setOpaque(true);
            statusPanel.setPreferredSize(new Dimension(10, 10));
            statusPanel.setBackground(SUCCESS_COLOR);
            
            // Make it round
            statusPanel.setBorder(new Border() {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(SUCCESS_COLOR);
                    g2.fillOval(x, y, width, height);
                    g2.dispose();
                }

                @Override
                public Insets getBorderInsets(Component c) {
                    return new Insets(0, 0, 0, 0);
                }

                @Override
                public boolean isBorderOpaque() {
                    return true;
                }
            });
            
            JPanel indicatorContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            indicatorContainer.setOpaque(false);
            indicatorContainer.add(statusPanel);
            
            userPanel.add(indicatorContainer, BorderLayout.EAST);
            
            // Make the panel full width
            userPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, userPanel.getPreferredSize().height));
            userListPanel.add(userPanel);
        }
        
        userListPanel.revalidate();
        userListPanel.repaint();
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    isConnected = false;
                    cardLayout.show(mainPanel, "login");
                    setTitle("Chat Client");
                    JOptionPane.showMessageDialog(ChatClientGUI.this, 
                            "Server đã đóng kết nối!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    onlineUsers.clear();
                }
            }
        });
    }

    private void sendMessage() {
        String message = tfMessage.getText().trim();
        if (!message.isEmpty() && isConnected) {
            client.sendMessage(message);
            tfMessage.setText("");
            tfMessage.requestFocus();
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
                        "[%s] %s", 
                        timeStamp, 
                        message);
                
                // Check if this is a join/leave message
                Style styleToUse = systemStyle;
                if (message.contains(" đã tham gia chat!")) {
                    styleToUse = joinLeaveStyle;
                    // Extract username
                    String username = message.substring(0, message.indexOf(" đã tham gia chat!"));
                    updateUserList(username, true);
                } else if (message.contains(" đã rời chat!")) {
                    styleToUse = joinLeaveStyle;
                    StyleConstants.setForeground(joinLeaveStyle, LEAVE_COLOR);
                    // Extract username
                    String username = message.substring(0, message.indexOf(" đã rời chat!"));
                    updateUserList(username, false);
                    // Reset style color for next join message
                    StyleConstants.setForeground(joinLeaveStyle, JOIN_COLOR);
                }
                
                // Thêm tin nhắn vào chatDocument với style hệ thống
                try {
                    int start = chatDocument.getLength();
                    chatDocument.insertString(start, formattedMessage + "\n", styleToUse);
                    
                    // Apply paragraph alignment
                    SimpleAttributeSet center = new SimpleAttributeSet();
                    StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
                    chatDocument.setParagraphAttributes(start, chatDocument.getLength() - start, center, false);
                } catch (BadLocationException e) {
                    logger.log(Level.WARNING, "Error displaying system message", e);
                    // Fallback: thêm văn bản thông thường
                    tpChat.setText(tpChat.getText() + formattedMessage + "\n");
                }
                
                // Cuộn xuống cuối
                tpChat.setCaretPosition(chatDocument.getLength());
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
                
                // Create message bubble style with right alignment
                try {
                    // Insert a new paragraph
                    int start = chatDocument.getLength();
                    chatDocument.insertString(start, "\n", null);
                    
                    // Insert timestamp (right aligned)
                    start = chatDocument.getLength();
                    chatDocument.insertString(start, timeStamp + " ", systemStyle);
                    
                    // Apply right alignment to timestamp
                    SimpleAttributeSet right = new SimpleAttributeSet();
                    StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
                    chatDocument.setParagraphAttributes(start, chatDocument.getLength() - start, right, false);
                    
                    // Insert a new paragraph for the message
                    start = chatDocument.getLength();
                    chatDocument.insertString(start, "\n", null);
                    
                    // Create message bubble style
                    Style bubbleStyle = tpChat.addStyle("MyBubble", null);
                    StyleConstants.setForeground(bubbleStyle, PRIMARY_DARK_COLOR);
                    StyleConstants.setFontFamily(bubbleStyle, "Segoe UI");
                    StyleConstants.setFontSize(bubbleStyle, 14);
                    StyleConstants.setBold(bubbleStyle, true);
                    StyleConstants.setBackground(bubbleStyle, MY_MESSAGE_BG);
                    
                    // Insert the message with bubble style
                    start = chatDocument.getLength();
                    chatDocument.insertString(start, message + "\n", bubbleStyle);
                    
                    // Apply right alignment to message
                    chatDocument.setParagraphAttributes(start, chatDocument.getLength() - start, right, false);
                } catch (BadLocationException e) {
                    logger.log(Level.WARNING, "Error displaying sent message", e);
                    // Fallback
                    tpChat.setText(tpChat.getText() + "[" + timeStamp + "] " + message + "\n");
                }
                
                // Cuộn xuống cuối
                tpChat.setCaretPosition(chatDocument.getLength());
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
                
                // Parse the sender and message content
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
                
                // Create message bubble style with left alignment
                try {
                    // Insert a new paragraph
                    int start = chatDocument.getLength();
                    chatDocument.insertString(start, "\n", null);
                    
                    // Insert sender name and timestamp (left aligned)
                    start = chatDocument.getLength();
                    chatDocument.insertString(start, sender + " " + timeStamp + "\n", systemStyle);
                    
                    // Apply left alignment to sender and timestamp
                    SimpleAttributeSet left = new SimpleAttributeSet();
                    StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);
                    chatDocument.setParagraphAttributes(start, chatDocument.getLength() - start, left, false);
                    
                    // Create message bubble style
                    Style bubbleStyle = tpChat.addStyle("OtherBubble", null);
                    StyleConstants.setForeground(bubbleStyle, TEXT_COLOR);
                    StyleConstants.setFontFamily(bubbleStyle, "Segoe UI");
                    StyleConstants.setFontSize(bubbleStyle, 14);
                    StyleConstants.setBackground(bubbleStyle, OTHER_MESSAGE_BG);
                    
                    // Insert the message with bubble style
                    start = chatDocument.getLength();
                    chatDocument.insertString(start, content + "\n", bubbleStyle);
                    
                    // Apply left alignment to message
                    chatDocument.setParagraphAttributes(start, chatDocument.getLength() - start, left, false);
                } catch (BadLocationException e) {
                    logger.log(Level.WARNING, "Error displaying received message", e);
                    // Fallback
                    tpChat.setText(tpChat.getText() + "[" + timeStamp + "] " + message + "\n");
                }
                
                // Cuộn xuống cuối
                tpChat.setCaretPosition(chatDocument.getLength());
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