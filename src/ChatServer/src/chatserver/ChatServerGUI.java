package chatserver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServerGUI extends JFrame {
    private JTextField tfPort;
    private JTextArea taLog;
    private JButton btnStart, btnStop, btnClear, btnViewUsers, btnViewMessages, btnExportData, btnDeleteData;
    private JLabel lblStatus, lblClientCount;
    private ChatServer server;
    private JTabbedPane tabbedPane;
    private static final Logger logger = Logger.getLogger(ChatServerGUI.class.getName());
    
    private final Color COLOR_PRIMARY = new Color(63, 81, 181);
    private final Color COLOR_PRIMARY_LIGHT = new Color(121, 134, 203);
    private final Color COLOR_PRIMARY_DARK = new Color(48, 63, 159);
    private final Color COLOR_SUCCESS = new Color(76, 175, 80);
    private final Color COLOR_ERROR = new Color(244, 67, 54);
    private final Color COLOR_WARNING = new Color(255, 152, 0);
    private final Color COLOR_INFO = new Color(3, 169, 244);
    private final Color COLOR_BACKGROUND = new Color(245, 245, 245);
    private final Color COLOR_CARD = new Color(255, 255, 255);
    
    private final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 18);
    private final Font FONT_SUBHEADER = new Font("Segoe UI", Font.BOLD, 15);
    private final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    private final Font FONT_MONOSPACE = new Font("Consolas", Font.PLAIN, 13);
    
    public ChatServerGUI() {
        server = new ChatServer(this);
        initComponents();
        customizeAppearance();
    }
    
    private void customizeAppearance() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    
                    UIManager.put("nimbusBase", COLOR_PRIMARY);
                    UIManager.put("nimbusBlueGrey", COLOR_BACKGROUND);
                    UIManager.put("control", COLOR_BACKGROUND);
                    
                    SwingUtilities.updateComponentTreeUI(this);
                    break;
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Không thể thiết lập giao diện hệ thống", ex);
        }
        
        lblStatus.setFont(FONT_NORMAL);
        lblStatus.setForeground(COLOR_ERROR);
        
        lblClientCount.setFont(FONT_NORMAL);
        
        btnStart.setFont(FONT_NORMAL);
        btnStart.setBackground(COLOR_SUCCESS);
        btnStart.setForeground(Color.WHITE);
        btnStart.setBorderPainted(false);
        btnStart.setFocusPainted(false);
        
        btnStop.setFont(FONT_NORMAL);
        btnStop.setBackground(COLOR_ERROR);
        btnStop.setForeground(Color.WHITE);
        btnStop.setBorderPainted(false);
        btnStop.setFocusPainted(false);
        
        btnClear.setFont(FONT_NORMAL);
        btnClear.setBackground(COLOR_INFO);
        btnClear.setForeground(Color.WHITE);
        btnClear.setBorderPainted(false);
        btnClear.setFocusPainted(false);
        
        btnDeleteData.setFont(FONT_NORMAL);
        btnDeleteData.setBackground(COLOR_WARNING);
        btnDeleteData.setForeground(Color.WHITE);
        btnDeleteData.setBorderPainted(false);
        btnDeleteData.setFocusPainted(false);
        
        // Thêm hiệu ứng hover cho các nút
        addButtonHoverEffect(btnStart, COLOR_SUCCESS);
        addButtonHoverEffect(btnStop, COLOR_ERROR);
        addButtonHoverEffect(btnClear, COLOR_INFO);
        addButtonHoverEffect(btnDeleteData, COLOR_WARNING);
        addButtonHoverEffect(btnViewUsers, COLOR_PRIMARY);
        addButtonHoverEffect(btnViewMessages, COLOR_PRIMARY);
        addButtonHoverEffect(btnExportData, COLOR_PRIMARY);
        
        taLog.setFont(FONT_MONOSPACE);
        taLog.setBackground(COLOR_CARD);
        
        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            }
        }
    }
    
    private void addButtonHoverEffect(JButton button, Color baseColor) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(baseColor.darker());
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(baseColor);
                }
            }
        });
    }
    
    private void initComponents() {
        setTitle("Chat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(COLOR_BACKGROUND);
        setContentPane(mainPanel);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_NORMAL);
        
        JPanel consolePanel = createConsolePanel();
        tabbedPane.addTab("Console", null, consolePanel, "Hiển thị thông tin hoạt động của server");
        
        JPanel settingsPanel = createSettingsPanel();
        tabbedPane.addTab("Cài đặt", null, settingsPanel, "Thay đổi cài đặt server");
        
        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);
        
        setupEventHandlers();
        
        setupGlobalHotkeys();
    }
    
    private void setupGlobalHotkeys() {
        // Phím tắt Ctrl+S: Khởi động server
        KeyStroke startKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        getRootPane().registerKeyboardAction(e -> {
            if (btnStart.isEnabled()) {
                startServer();
            }
        }, startKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        // Phím tắt Ctrl+T: Dừng server
        KeyStroke stopKey = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
        getRootPane().registerKeyboardAction(e -> {
            if (btnStop.isEnabled()) {
                stopServer();
            }
        }, stopKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        // Phím tắt Ctrl+L: Xóa log
        KeyStroke clearKey = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);
        getRootPane().registerKeyboardAction(e -> {
            taLog.setText("");
        }, clearKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        // Phím tắt F1: Hiển thị trợ giúp
        KeyStroke helpKey = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
        getRootPane().registerKeyboardAction(e -> {
            showHelpDialog();
        }, helpKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    
    private void showHelpDialog() {
        JDialog helpDialog = new JDialog(this, "Hướng dẫn sử dụng", true);
        helpDialog.setLayout(new BorderLayout(10, 10));
        helpDialog.setSize(600, 500);
        helpDialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JTextArea helpText = new JTextArea();
        helpText.setEditable(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setFont(FONT_NORMAL);
        helpText.setText(
            "HƯỚNG DẪN SỬ DỤNG CHAT SERVER\n\n" +
            "1. KHỞI ĐỘNG SERVER\n" +
            "   - Nhập số port (1024-65535) vào ô Port\n" +
            "   - Nhấn nút 'Khởi động' hoặc nhấn Ctrl+S\n\n" +
            "2. DỪNG SERVER\n" +
            "   - Nhấn nút 'Dừng' hoặc nhấn Ctrl+T\n" +
            "   - Lưu ý: Tất cả client sẽ bị ngắt kết nối\n\n" +
            "3. XEM DANH SÁCH NGƯỜI DÙNG\n" +
            "   - Nhấn nút 'Xem người dùng'\n\n" +
            "4. XEM TIN NHẮN\n" +
            "   - Nhấn nút 'Xem tin nhắn'\n" +
            "   - Có thể tìm kiếm tin nhắn bằng từ khóa\n\n" +
            "5. XUẤT DỮ LIỆU\n" +
            "   - Nhấn nút 'Xuất dữ liệu'\n" +
            "   - Chọn vị trí lưu file\n\n" +
            "6. XÓA DỮ LIỆU\n" +
            "   - Nhấn nút 'Xóa dữ liệu port'\n" +
            "   - Xác nhận xóa (không thể hoàn tác)\n\n" +
            "7. XÓA LOG\n" +
            "   - Nhấn nút 'Xóa log' hoặc Ctrl+L\n\n" +
            "PHÍM TẮT:\n" +
            "- Ctrl+S: Khởi động server\n" +
            "- Ctrl+T: Dừng server\n" +
            "- Ctrl+L: Xóa log\n" +
            "- F1: Hiển thị hướng dẫn này\n"
        );
        
        JScrollPane scrollPane = new JScrollPane(helpText);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        JButton closeButton = new JButton("Đóng");
        closeButton.addActionListener(e -> helpDialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        helpDialog.add(contentPanel);
        helpDialog.setVisible(true);
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Điều khiển Server", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_SUBHEADER));
        controlPanel.setBackground(COLOR_BACKGROUND);
        
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(COLOR_BACKGROUND);
        
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portPanel.setBackground(COLOR_BACKGROUND);
        JLabel lblPort = new JLabel("Port:");
        lblPort.setFont(FONT_NORMAL);
        portPanel.add(lblPort);
        tfPort = new JTextField("12345", 8);
        tfPort.setFont(FONT_NORMAL);
        portPanel.add(tfPort);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(COLOR_BACKGROUND);
        
        btnStart = new JButton("Khởi động");
        btnStart.setFont(FONT_NORMAL);
        btnStart.setForeground(Color.WHITE);
        btnStart.setBackground(COLOR_SUCCESS);
        
        btnStop = new JButton("Dừng");
        btnStop.setFont(FONT_NORMAL);
        btnStop.setForeground(Color.WHITE);
        btnStop.setBackground(COLOR_ERROR);
        btnStop.setEnabled(false);
        
        btnClear = new JButton("Xóa log");
        btnClear.setFont(FONT_NORMAL);
        btnClear.setForeground(Color.WHITE);
        btnClear.setBackground(COLOR_INFO);
        
        buttonPanel.add(btnStart);
        buttonPanel.add(btnStop);
        buttonPanel.add(btnClear);
        
        leftPanel.add(portPanel, BorderLayout.NORTH);
        leftPanel.add(buttonPanel, BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        rightPanel.setBackground(COLOR_BACKGROUND);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        
        JLabel lblStatusTitle = new JLabel("Trạng thái:");
        lblStatusTitle.setFont(FONT_NORMAL);
        rightPanel.add(lblStatusTitle);
        
        lblStatus = new JLabel("Đã dừng");
        lblStatus.setForeground(COLOR_ERROR);
        rightPanel.add(lblStatus);
        
        JLabel lblClientsTitle = new JLabel("Clients hiện tại:");
        lblClientsTitle.setFont(FONT_NORMAL);
        rightPanel.add(lblClientsTitle);
        
        lblClientCount = new JLabel("0");
        lblClientCount.setForeground(COLOR_INFO);
        rightPanel.add(lblClientCount);
        
        controlPanel.add(leftPanel, BorderLayout.WEST);
        controlPanel.add(rightPanel, BorderLayout.CENTER);
        
        return controlPanel;
    }
    
    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BACKGROUND);
        
        JPanel functionPanel = new JPanel();
        functionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Chức năng", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_SUBHEADER));
        functionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        functionPanel.setBackground(COLOR_BACKGROUND);
        
        btnViewUsers = new JButton("Xem người dùng");
        btnViewUsers.setFont(FONT_NORMAL);
        btnViewUsers.setEnabled(false);
        
        btnViewMessages = new JButton("Xem tin nhắn");
        btnViewMessages.setFont(FONT_NORMAL);
        btnViewMessages.setEnabled(false);
        
        btnExportData = new JButton("Xuất dữ liệu");
        btnExportData.setFont(FONT_NORMAL);
        btnExportData.setEnabled(false);
        
        btnDeleteData = new JButton("Xóa dữ liệu port");
        btnDeleteData.setFont(FONT_NORMAL);
        btnDeleteData.setForeground(Color.WHITE);
        btnDeleteData.setBackground(COLOR_WARNING);
        btnDeleteData.setEnabled(false);
        
        functionPanel.add(btnViewUsers);
        functionPanel.add(btnViewMessages);
        functionPanel.add(btnExportData);
        functionPanel.add(btnDeleteData);
        
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Lịch sử hoạt động", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_SUBHEADER));
        logPanel.setBackground(COLOR_BACKGROUND);
        
        taLog = new JTextArea();
        taLog.setEditable(false);
        taLog.setLineWrap(true);
        taLog.setWrapStyleWord(true);
        taLog.setFont(FONT_MONOSPACE);
        taLog.setBackground(new Color(252, 252, 252));
        
        DefaultCaret caret = (DefaultCaret) taLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane scrollPane = new JScrollPane(taLog);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(COLOR_BACKGROUND);
        JLabel lblFilter = new JLabel("Lọc log:");
        lblFilter.setFont(FONT_NORMAL);
        JTextField tfFilter = new JTextField(20);
        JButton btnFilter = new JButton("Lọc");
        JButton btnClearFilter = new JButton("Xóa lọc");
        
        filterPanel.add(lblFilter);
        filterPanel.add(tfFilter);
        filterPanel.add(btnFilter);
        filterPanel.add(btnClearFilter);
        
        logPanel.add(filterPanel, BorderLayout.NORTH);
        
        btnFilter.addActionListener(e -> {
            String filterText = tfFilter.getText().trim();
            if (!filterText.isEmpty()) {
                filterLog(filterText);
            }
        });
        
        btnClearFilter.addActionListener(e -> {
            tfFilter.setText("");
        });
        
        panel.add(functionPanel, BorderLayout.NORTH);
        panel.add(logPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void filterLog(String filterText) {
        JDialog filterDialog = new JDialog(this, "Kết quả lọc log với từ khóa: " + filterText, false);
        filterDialog.setSize(800, 500);
        filterDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JTextArea filteredLog = new JTextArea();
        filteredLog.setEditable(false);
        filteredLog.setLineWrap(true);
        filteredLog.setWrapStyleWord(true);
        filteredLog.setFont(FONT_MONOSPACE);
        
        JScrollPane scrollPane = new JScrollPane(filteredLog);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        String[] lines = taLog.getText().split("\n");
        int matchCount = 0;
        
        for (String line : lines) {
            if (line.toLowerCase().contains(filterText.toLowerCase())) {
                filteredLog.append(line + "\n");
                matchCount++;
            }
        }
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblInfo = new JLabel("Tìm thấy " + matchCount + " dòng phù hợp");
        infoPanel.add(lblInfo);
        panel.add(infoPanel, BorderLayout.NORTH);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnExport = new JButton("Xuất kết quả");
        JButton btnClose = new JButton("Đóng");
        
        btnExport.addActionListener(e -> {
            exportFilteredLog(filteredLog.getText(), filterText);
        });
        
        btnClose.addActionListener(e -> filterDialog.dispose());
        
        buttonPanel.add(btnExport);
        buttonPanel.add(btnClose);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        filterDialog.add(panel);
        filterDialog.setVisible(true);
    }
    
    private void exportFilteredLog(String logContent, String filterText) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất log đã lọc");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files", "txt"));
        
        String defaultFileName = "filtered_log_" + filterText.replaceAll("[^a-zA-Z0-9]", "_") + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        fileChooser.setSelectedFile(new File(defaultFileName));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.endsWith(".txt")) {
                filePath += ".txt";
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                writer.println("=== LOG ĐÃ LỌC THEO TỪ KHÓA: " + filterText + " ===");
                writer.println("Thời gian xuất: " + new Date());
                writer.println();
                writer.println(logContent);
                
                JOptionPane.showMessageDialog(this, 
                    "Đã xuất log đã lọc ra file: " + filePath, 
                    "Xuất thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Lỗi khi xuất file: " + ex.getMessage(), 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BACKGROUND);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel settingsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        settingsPanel.setBackground(COLOR_BACKGROUND);
        settingsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Cài đặt chung", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_SUBHEADER));
        
        settingsPanel.add(new JLabel("Tự động xóa log cũ:"));
        JCheckBox cbAutoCleanLog = new JCheckBox("Bật");
        settingsPanel.add(cbAutoCleanLog);
        
        settingsPanel.add(new JLabel("Số lượng tin nhắn lưu trữ tối đa:"));
        JTextField tfMaxMessages = new JTextField("1000");
        settingsPanel.add(tfMaxMessages);
        
        settingsPanel.add(new JLabel("Tự động sao lưu dữ liệu:"));
        JCheckBox cbAutoBackup = new JCheckBox("Bật");
        settingsPanel.add(cbAutoBackup);
        
        settingsPanel.add(new JLabel("Thời gian giữa các lần sao lưu (phút):"));
        JTextField tfBackupInterval = new JTextField("60");
        settingsPanel.add(tfBackupInterval);
        
        settingsPanel.add(new JLabel("Thư mục xuất dữ liệu mặc định:"));
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.setBackground(COLOR_BACKGROUND);
        JTextField tfExportPath = new JTextField("database");
        JButton btnBrowse = new JButton("...");
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                tfExportPath.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        pathPanel.add(tfExportPath, BorderLayout.CENTER);
        pathPanel.add(btnBrowse, BorderLayout.EAST);
        settingsPanel.add(pathPanel);
        
        settingsPanel.add(new JLabel("Khóa mã hóa Vigenere:"));
        JTextField tfEncryptionKey = new JTextField("CHATAPP");
        settingsPanel.add(tfEncryptionKey);
        
        JButton btnSaveSettings = new JButton("Lưu cài đặt");
        btnSaveSettings.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Đã lưu cài đặt thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(COLOR_BACKGROUND);
        buttonPanel.add(btnSaveSettings);
        
        panel.add(settingsPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.setBackground(new Color(240, 240, 240));
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(null);
        JLabel serverStatusLabel = new JLabel("Trạng thái: Chưa khởi động");
        serverStatusLabel.setFont(FONT_SMALL);
        leftPanel.add(serverStatusLabel);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(null);
        
        JLabel lblVersion = new JLabel("v1.0.0");
        lblVersion.setFont(FONT_SMALL);
        rightPanel.add(lblVersion);
        
        statusBar.add(leftPanel, BorderLayout.WEST);
        statusBar.add(rightPanel, BorderLayout.EAST);
        
        return statusBar;
    }
    
    private void setupEventHandlers() {
        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
        btnClear.addActionListener(e -> taLog.setText(""));
        btnViewUsers.addActionListener(e -> showUsersDialog());
        btnViewMessages.addActionListener(e -> showMessagesDialog());
        btnExportData.addActionListener(e -> exportData());
        btnDeleteData.addActionListener(e -> deletePortData());
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (btnStop.isEnabled()) {
                    int option = JOptionPane.showConfirmDialog(
                            ChatServerGUI.this,
                            "Server đang chạy. Bạn có muốn dừng server trước khi thoát?",
                            "Xác nhận thoát",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    
                    if (option == JOptionPane.YES_OPTION) {
                        server.stop();
                    } else if (option == JOptionPane.CANCEL_OPTION) {
                        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                        return;
                    }
                }
                
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });
    }
    
    private void deletePortData() {
        if (!server.isRunning()) {
            JOptionPane.showMessageDialog(this, "Server chưa được khởi động!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc chắn muốn xóa toàn bộ dữ liệu trên port " + server.getServerPort() + "?\n" +
                "Hành động này không thể hoàn tác!", 
                "Xác nhận xóa dữ liệu", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            JDialog progressDialog = new JDialog(this, "Đang xóa dữ liệu...", true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(true);
            progressBar.setString("Đang xóa dữ liệu trong cơ sở dữ liệu...");
            progressDialog.add(progressBar);
            progressDialog.setSize(300, 80);
            progressDialog.setLocationRelativeTo(this);
            
            new Thread(() -> {
                SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
            }).start();
            
            new Thread(() -> {
                server.deletePortData();
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(this, 
                        "Đã xóa toàn bộ dữ liệu của port " + server.getServerPort(), 
                        "Xóa dữ liệu thành công", JOptionPane.INFORMATION_MESSAGE);
                });
            }).start();
        }
    }
    
    private void exportData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn vị trí lưu file");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files", "txt"));
        
        File defaultDir = new File("database");
        if (!defaultDir.exists()) {
            defaultDir.mkdir();
        }
        fileChooser.setCurrentDirectory(defaultDir);
        
        String defaultFileName = "chat_port_" + server.getServerPort() + "_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        fileChooser.setSelectedFile(new File(defaultFileName));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.endsWith(".txt")) {
                filePath += ".txt";
            }
            
            JDialog progressDialog = new JDialog(this, "Đang xuất dữ liệu...", true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(true);
            progressBar.setString("Đang xuất dữ liệu ra file văn bản...");
            progressDialog.add(progressBar);
            progressDialog.setSize(300, 80);
            progressDialog.setLocationRelativeTo(this);
            
            final String finalPath = filePath;
            
            new Thread(() -> {
                SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
            }).start();
            
            new Thread(() -> {
                server.getDatabaseManager().exportDataToTextFileByPort(finalPath, server.getServerPort());
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(this, 
                        "Dữ liệu chat trên port " + server.getServerPort() + " đã được xuất ra file: " + finalPath, 
                        "Xuất dữ liệu thành công", JOptionPane.INFORMATION_MESSAGE);
                });
            }).start();
        }
    }
    
    private void showUsersDialog() {
        if (!server.isRunning()) {
            JOptionPane.showMessageDialog(this, "Server chưa được khởi động!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(this, "Danh sách người dùng trên port " + server.getServerPort(), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(550, 400);
        dialog.setLocationRelativeTo(this);
        
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        model.addColumn("STT");
        model.addColumn("Tên người dùng");
        model.addColumn("Lần đăng nhập gần nhất");
        model.addColumn("Trạng thái");
        
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.setRowHeight(25);
        table.setFont(FONT_NORMAL);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        
        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(COLOR_BACKGROUND);
        JLabel lblInfo = new JLabel("Danh sách người dùng");
        lblInfo.setFont(FONT_NORMAL);
        infoPanel.add(lblInfo);
        dialog.add(infoPanel, BorderLayout.NORTH);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(COLOR_BACKGROUND);
        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(FONT_NORMAL);
        JButton btnExport = new JButton("Xuất danh sách");
        btnExport.setFont(FONT_NORMAL);
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(FONT_NORMAL);
        
        btnRefresh.addActionListener(e -> {
            model.setRowCount(0);
            loadUsersData(model);
        });
        
        btnExport.addActionListener(e -> {
            exportUsersList(table);
        });
        
        btnClose.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnExport);
        buttonPanel.add(btnClose);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        loadUsersData(model);
        
        dialog.setVisible(true);
    }
    
    private void exportUsersList(JTable table) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất danh sách người dùng");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files", "txt"));
        fileChooser.setSelectedFile(new File("users_port_" + server.getServerPort() + "_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.endsWith(".txt")) {
                filePath += ".txt";
            }
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                writer.println("=== DANH SÁCH NGƯỜI DÙNG TRÊN PORT " + server.getServerPort() + " ===");
                writer.println("Thời gian xuất: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.println();
                
                writer.println(String.format("%-5s %-20s %-30s %-15s", "STT", "Tên người dùng", "Lần đăng nhập gần nhất", "Trạng thái"));
                writer.println(String.format("%-5s %-20s %-30s %-15s", "-----", "--------------------", "------------------------------", "---------------"));
                
                for (int i = 0; i < table.getRowCount(); i++) {
                    String stt = table.getValueAt(i, 0).toString();
                    String username = table.getValueAt(i, 1).toString();
                    String lastLogin = table.getValueAt(i, 2).toString();
                    String status = table.getValueAt(i, 3).toString();
                    
                    writer.println(String.format("%-5s %-20s %-30s %-15s", stt, username, lastLogin, status));
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Danh sách người dùng đã được xuất ra file: " + filePath, 
                    "Xuất thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Lỗi khi xuất file: " + ex.getMessage(), 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadUsersData(DefaultTableModel model) {
        try {
            ResultSet rs = server.getDatabaseManager().getActiveUsers(server.getServerPort());
            boolean hasUsers = false;
            int rowCount = 0;
            
            while (rs.next()) {
                hasUsers = true;
                rowCount++;
                String username = rs.getString("username");
                String lastLogin = rs.getString("last_login");
                boolean online = false;
                
                // Kiểm tra xem user này có đang online không
                synchronized (server.clients) {
                    for (ClientHandler client : server.clients) {
                        if (client.getUsername() != null && client.getUsername().equals(username) && client.isConnected()) {
                            online = true;
                            break;
                        }
                    }
                }
                
                String status = online ? "Online" : "Offline";
                
                model.addRow(new Object[]{rowCount, username, lastLogin, status});
            }
            
            if (!hasUsers) {
                model.addRow(new Object[]{"", "Không có người dùng nào đã kết nối đến port này.", "", ""});
            }
            
            rs.close();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Không thể lấy danh sách người dùng", ex);
            JOptionPane.showMessageDialog(this, "Lỗi khi lấy dữ liệu: " + ex.getMessage(), 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showMessagesDialog() {
        if (!server.isRunning()) {
            JOptionPane.showMessageDialog(this, "Server chưa được khởi động!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(this, "Lịch sử tin nhắn trên port " + server.getServerPort(), true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(750, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchPanel.setBackground(COLOR_BACKGROUND);
        
        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(FONT_NORMAL);
        
        JTextField tfSearch = new JTextField(25);
        tfSearch.setFont(FONT_NORMAL);
        
        JButton btnSearch = new JButton("Tìm");
        btnSearch.setFont(FONT_NORMAL);
        
        JButton btnClearSearch = new JButton("Xóa");
        btnClearSearch.setFont(FONT_NORMAL);
        
        searchPanel.add(lblSearch);
        searchPanel.add(tfSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClearSearch);
        
        dialog.add(searchPanel, BorderLayout.NORTH);
        
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        model.addColumn("STT");
        model.addColumn("Thời gian");
        model.addColumn("Người dùng");
        model.addColumn("Tin nhắn gốc");
        model.addColumn("Tin nhắn mã hóa");
        
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(250);
        table.getColumnModel().getColumn(4).setPreferredWidth(250);
        table.setRowHeight(25);
        table.setFont(FONT_NORMAL);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        
        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(COLOR_BACKGROUND);
        JLabel lblInfo = new JLabel("Lịch sử tin nhắn");
        lblInfo.setFont(FONT_NORMAL);
        infoPanel.add(lblInfo);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(COLOR_BACKGROUND);
        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(FONT_NORMAL);
        JButton btnExport = new JButton("Xuất tin nhắn");
        btnExport.setFont(FONT_NORMAL);
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(FONT_NORMAL);
        
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnExport);
        buttonPanel.add(btnClose);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBackground(COLOR_BACKGROUND);
        southPanel.add(infoPanel, BorderLayout.WEST);
        southPanel.add(buttonPanel, BorderLayout.EAST);
        dialog.add(southPanel, BorderLayout.SOUTH);
        
        loadMessagesData(model, null);
        
        ActionListener searchAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = tfSearch.getText().trim();
                loadMessagesData(model, searchText.isEmpty() ? null : searchText);
            }
        };
        
        btnSearch.addActionListener(searchAction);
        tfSearch.addActionListener(searchAction);
        
        btnClearSearch.addActionListener(e -> {
            tfSearch.setText("");
            loadMessagesData(model, null);
        });
        
        btnRefresh.addActionListener(e -> {
            String searchText = tfSearch.getText().trim();
            loadMessagesData(model, searchText.isEmpty() ? null : searchText);
        });
        
        btnExport.addActionListener(e -> {
            String searchText = tfSearch.getText().trim();
            exportFilteredMessages(searchText);
        });
        
        btnClose.addActionListener(e -> dialog.dispose());
        
        dialog.setVisible(true);
    }
    
    private void loadMessagesData(DefaultTableModel model, String searchText) {
        model.setRowCount(0);
        
        try {
            ResultSet rs;
            
            if (searchText == null || searchText.isEmpty()) {
                rs = server.getDatabaseManager().getMessagesWithEncryption(500, server.getServerPort());
            } else {
                rs = server.getDatabaseManager().searchMessagesWithEncryption(searchText, server.getServerPort());
            }
            
            boolean hasMessages = false;
            int rowCount = 0;
            
            while (rs.next()) {
                hasMessages = true;
                rowCount++;
                String username = rs.getString("username");
                String message = rs.getString("message");
                String originalMsg = rs.getString("original_message");
                String encryptedMsg = rs.getString("encrypted_message");
                String timestamp = rs.getString("timestamp");
                
                if (originalMsg == null || originalMsg.isEmpty()) originalMsg = message;
                if (encryptedMsg == null || encryptedMsg.isEmpty()) encryptedMsg = message;
                
                model.addRow(new Object[]{rowCount, timestamp, username, originalMsg, encryptedMsg});
            }
            
            if (!hasMessages) {
                if (searchText != null && !searchText.isEmpty()) {
                    model.addRow(new Object[]{"", "", "Không tìm thấy tin nhắn phù hợp.", "", ""});
                } else {
                    model.addRow(new Object[]{"", "", "Chưa có tin nhắn nào trên port này.", "", ""});
                }
            }
            
            rs.close();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Không thể lấy danh sách tin nhắn", ex);
            JOptionPane.showMessageDialog(this, "Lỗi khi lấy dữ liệu: " + ex.getMessage(), 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void exportFilteredMessages(String searchText) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất tin nhắn");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text files", "txt"));
        
        File defaultDir = new File("database");
        if (!defaultDir.exists()) {
            defaultDir.mkdir();
        }
        fileChooser.setCurrentDirectory(defaultDir);
        
        String searchSuffix = searchText != null && !searchText.isEmpty() ? "_search_" + searchText.replaceAll("[^a-zA-Z0-9]", "_") : "";
        String defaultFileName = "messages_port_" + server.getServerPort() + searchSuffix + "_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        fileChooser.setSelectedFile(new File(defaultFileName));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.endsWith(".txt")) {
                filePath += ".txt";
            }
            
            JDialog progressDialog = new JDialog(this, "Đang xuất tin nhắn...", true);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(true);
            progressBar.setString("Đang xuất dữ liệu ra file văn bản...");
            progressDialog.add(progressBar);
            progressDialog.setSize(300, 80);
            progressDialog.setLocationRelativeTo(this);
            
            final String finalPath = filePath;
            final String finalSearchText = searchText;
            
            new Thread(() -> {
                SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
            }).start();
            
            new Thread(() -> {
                if (finalSearchText != null && !finalSearchText.isEmpty()) {
                    server.getDatabaseManager().exportFilteredMessagesToFile(finalPath, finalSearchText, server.getServerPort());
                    
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(this, 
                            "Tin nhắn đã lọc với từ khóa '" + finalSearchText + "' đã được xuất ra file: " + finalPath, 
                            "Xuất dữ liệu thành công", JOptionPane.INFORMATION_MESSAGE);
                    });
                } else {
                    server.getDatabaseManager().exportDataToTextFileByPort(finalPath, server.getServerPort());
                    
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(this, 
                            "Tất cả tin nhắn trên port " + server.getServerPort() + " đã được xuất ra file: " + finalPath, 
                            "Xuất dữ liệu thành công", JOptionPane.INFORMATION_MESSAGE);
                    });
                }
            }).start();
        }
    }
    
    private void startServer() {
        String portText = tfPort.getText().trim();
        
        if (portText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập port!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                JOptionPane.showMessageDialog(this, 
                        "Port không hợp lệ! Vui lòng nhập số từ 1024 đến 65535.", 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            JDialog progressDialog = new JDialog(this, "Đang khởi động server...", true);
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(true);
            progressBar.setString("Đang kết nối đến cơ sở dữ liệu...");
            panel.add(progressBar, BorderLayout.CENTER);
            
            JLabel lblProgress = new JLabel("Đang khởi tạo server...");
            lblProgress.setFont(FONT_SMALL);
            panel.add(lblProgress, BorderLayout.SOUTH);
            
            progressDialog.add(panel);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);
            
            new Thread(() -> {
                SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
            }).start();
            
            new Thread(() -> {
                boolean success = server.start(port);
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    
                    if (success) {
                        updateUIOnStartSuccess();
                    } else {
                        JOptionPane.showMessageDialog(this, 
                                "Không thể khởi động server trên port " + port + "!\n" +
                                "Có thể port đã được sử dụng hoặc không có quyền truy cập.", 
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ! Vui lòng nhập một số.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateUIOnStartSuccess() {
        tfPort.setEnabled(false);
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        btnViewUsers.setEnabled(true);
        btnViewMessages.setEnabled(true);
        btnExportData.setEnabled(true);
        btnDeleteData.setEnabled(true);
        lblStatus.setText("Đang chạy");
        lblStatus.setForeground(COLOR_SUCCESS);
        
        updateClientCount(0);
        
        setTitle("Chat Server - Port: " + server.getServerPort());
        
        tabbedPane.setSelectedIndex(0);
    }
    
    private void stopServer() {
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc chắn muốn dừng server?\n" +
                "Tất cả client đang kết nối sẽ bị ngắt kết nối.",
                "Xác nhận dừng server", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            JDialog progressDialog = new JDialog(this, "Đang dừng server...", true);
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setStringPainted(true);
            progressBar.setString("Đang đóng kết nối và lưu dữ liệu...");
            panel.add(progressBar, BorderLayout.CENTER);
            
            JLabel lblProgress = new JLabel("Đang ngắt kết nối các client...");
            lblProgress.setFont(FONT_SMALL);
            panel.add(lblProgress, BorderLayout.SOUTH);
            
            progressDialog.add(panel);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);
            
            new Thread(() -> {
                SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
            }).start();
            
            new Thread(() -> {
                server.stop();
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    updateUIOnStop();
                });
            }).start();
        }
    }
    
    private void updateUIOnStop() {
        tfPort.setEnabled(true);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnViewUsers.setEnabled(false);
        btnViewMessages.setEnabled(false);
        btnExportData.setEnabled(false);
        btnDeleteData.setEnabled(false);
        lblStatus.setText("Đã dừng");
        lblStatus.setForeground(COLOR_ERROR);
        
        updateClientCount(0);
        setTitle("Chat Server");
        
        logMessage("Server đã dừng trên port " + server.getServerPort());
    }
    
    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStamp = sdf.format(new Date());
            taLog.append("[" + timeStamp + "] " + message + "\n");
        });
    }
    
    public void logCompareMessage(String username, String encryptedContent, String originalContent) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStamp = sdf.format(new Date());
            
            taLog.append("╔══════════════════════════════════════════════════════════════════════════╗\n");
            taLog.append("║ [" + timeStamp + "] " + username + " gửi tin nhắn:\n");
            taLog.append("║ ┌────────────────────────────────────────────────────────────────────┐\n");
            taLog.append("║ │ Nội dung gốc:    " + originalContent + "\n");
            taLog.append("║ │ Mã hóa Vigenere: " + encryptedContent + "\n");
            taLog.append("║ └────────────────────────────────────────────────────────────────────┘\n");
            taLog.append("╚══════════════════════════════════════════════════════════════════════════╝\n");
        });
    }
    
    public void updateClientCount(int count) {
        SwingUtilities.invokeLater(() -> {
            lblClientCount.setText(String.valueOf(count));
            
            if (server.isRunning()) {
                setTitle("Chat Server - Port: " + server.getServerPort() + " - Clients: " + count);
            }
        });
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Không thể thiết lập giao diện hệ thống", ex);
        }
        
        SwingUtilities.invokeLater(() -> {
            new ChatServerGUI().setVisible(true);
        });
    }
}