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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServerGUI extends JFrame {
    private JTextField tfPort;
    private JTextArea taLog;
    private JButton btnStart, btnStop, btnClear, btnViewUsers, btnViewMessages, btnExportData, btnDeleteData;
    private JLabel lblStatus, lblClientCount, lblTotalClients, lblDatabaseStatus, lblMessagesCount, lblUsersCount;
    private JLabel lblServerUptime, lblServerMemory, lblServerCPU;
    private ChatServer server;
    private JTabbedPane tabbedPane;
    private static final Logger logger = Logger.getLogger(ChatServerGUI.class.getName());
    private Timer uptimeTimer;
    private Timer resourceMonitorTimer;
    private Date startTime;
    private Map<String, Integer> chartData = new HashMap<>();
    private Color chartColor = new Color(65, 105, 225);
    private JPanel chartPanel;
    
    // Trạng thái cơ sở dữ liệu
    private boolean isDatabaseConnected = false;
    
    // Màu sắc cho giao diện
    private final Color COLOR_SUCCESS = new Color(0, 130, 0);
    private final Color COLOR_ERROR = new Color(180, 0, 0);
    private final Color COLOR_WARNING = new Color(200, 120, 0);
    private final Color COLOR_INFO = new Color(0, 0, 150);
    private final Color COLOR_BACKGROUND = new Color(240, 240, 240);
    private final Color COLOR_BACKGROUND_ALT = new Color(248, 248, 248);
    private final Font FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private final Font FONT_NORMAL = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final Font FONT_MONOSPACE = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    
    public ChatServerGUI() {
        server = new ChatServer(this);
        initializeTimers();
        initComponents();
        customizeAppearance();
    }
    
    private void initializeTimers() {
        // Timer để cập nhật thời gian uptime
        uptimeTimer = new Timer(1000, e -> {
            if (server.isRunning() && startTime != null) {
                updateUptimeLabel();
            }
        });
        
        // Timer để giám sát tài nguyên
        resourceMonitorTimer = new Timer(2000, e -> {
            if (server.isRunning()) {
                updateResourceUsage();
            }
        });
    }
    
    private void customizeAppearance() {
        // Thiết lập giao diện chung
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Không thể thiết lập giao diện hệ thống", ex);
        }
        
        // Thiết lập icon cho frame
        try {
            // Nếu có icon, thêm vào đây
            // setIconImage(new ImageIcon("path/to/icon.png").getImage());
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Không thể tải icon", ex);
        }
        
        // Thiết lập màu cho các label
        lblStatus.setFont(FONT_BOLD);
        lblClientCount.setFont(FONT_BOLD);
        lblTotalClients.setFont(FONT_BOLD);
        lblDatabaseStatus.setFont(FONT_BOLD);
        lblMessagesCount.setFont(FONT_BOLD);
        lblUsersCount.setFont(FONT_BOLD);
        lblServerUptime.setFont(FONT_BOLD);
        lblServerMemory.setFont(FONT_BOLD);
        lblServerCPU.setFont(FONT_BOLD);
        
        // Thiết lập tooltips
        tfPort.setToolTipText("Nhập port cho server (1024-65535)");
        btnStart.setToolTipText("Khởi động server trên port đã chỉ định");
        btnStop.setToolTipText("Dừng server và ngắt kết nối tất cả client");
        btnClear.setToolTipText("Xóa nội dung log trên màn hình");
        btnViewUsers.setToolTipText("Xem danh sách người dùng đã kết nối");
        btnViewMessages.setToolTipText("Xem lịch sử tin nhắn và tìm kiếm");
        btnExportData.setToolTipText("Xuất dữ liệu ra file text");
        btnDeleteData.setToolTipText("Xóa tất cả dữ liệu của port hiện tại");
        
        // Thiết lập màu nền cho các panel
        tabbedPane.setBackground(COLOR_BACKGROUND);
    }
    
    private void initComponents() {
        setTitle("Chat Server Monitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 700);
        setLocationRelativeTo(null);
        
        // Tạo panel chính với padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(COLOR_BACKGROUND);
        setContentPane(mainPanel);
        
        // Tạo tab cho các chức năng
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_NORMAL);
        
        // Tab Console
        JPanel consolePanel = createConsolePanel();
        tabbedPane.addTab("Console", null, consolePanel, "Hiển thị thông tin hoạt động của server");
        
        // Tab Thống kê
        JPanel statsPanel = createStatsPanel();
        tabbedPane.addTab("Thống kê", null, statsPanel, "Xem thống kê người dùng và tin nhắn");
        
        // Tab Cài đặt
        JPanel settingsPanel = createSettingsPanel();
        tabbedPane.addTab("Cài đặt", null, settingsPanel, "Thay đổi cài đặt server");
        
        // Tab Giới thiệu
        JPanel aboutPanel = createAboutPanel();
        tabbedPane.addTab("Giới thiệu", null, aboutPanel, "Thông tin về ứng dụng");
        
        // Thêm vào panel chính
        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Thêm thanh trạng thái
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);
        
        // Thiết lập sự kiện
        setupEventHandlers();
        
        // Thiết lập phím tắt toàn cục
        setupGlobalHotkeys();
    }
    
    private void setupGlobalHotkeys() {
        // Ctrl+S: Start Server
        KeyStroke startKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        getRootPane().registerKeyboardAction(e -> {
            if (btnStart.isEnabled()) {
                startServer();
            }
        }, startKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        // Ctrl+T: Stop Server
        KeyStroke stopKey = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
        getRootPane().registerKeyboardAction(e -> {
            if (btnStop.isEnabled()) {
                stopServer();
            }
        }, stopKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        // Ctrl+L: Clear Log
        KeyStroke clearKey = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);
        getRootPane().registerKeyboardAction(e -> {
            taLog.setText("");
        }, clearKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        // F1: Hiển thị hướng dẫn
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
                TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD));
        controlPanel.setBackground(COLOR_BACKGROUND);
        
        // Panel trái chứa thiết lập server
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(COLOR_BACKGROUND);
        
        // Panel port
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        portPanel.setBackground(COLOR_BACKGROUND);
        JLabel lblPort = new JLabel("Port:");
        lblPort.setFont(FONT_NORMAL);
        portPanel.add(lblPort);
        tfPort = new JTextField("12345", 8);
        tfPort.setFont(FONT_NORMAL);
        portPanel.add(tfPort);
        
        // Panel nút điều khiển
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(COLOR_BACKGROUND);
        
        // Nút khởi động với icon
        btnStart = new JButton("Khởi động");
        btnStart.setFont(FONT_NORMAL);
        btnStart.setForeground(COLOR_SUCCESS);
        
        // Nút dừng với icon
        btnStop = new JButton("Dừng");
        btnStop.setFont(FONT_NORMAL);
        btnStop.setForeground(COLOR_ERROR);
        btnStop.setEnabled(false);
        
        // Nút xóa log
        btnClear = new JButton("Xóa log");
        btnClear.setFont(FONT_NORMAL);
        
        buttonPanel.add(btnStart);
        buttonPanel.add(btnStop);
        buttonPanel.add(btnClear);
        
        leftPanel.add(portPanel, BorderLayout.NORTH);
        leftPanel.add(buttonPanel, BorderLayout.CENTER);
        
        // Panel phải chứa trạng thái
        JPanel rightPanel = new JPanel(new GridLayout(3, 2, 10, 5));
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
        
        JLabel lblTotalClientsTitle = new JLabel("Tổng số clients:");
        lblTotalClientsTitle.setFont(FONT_NORMAL);
        rightPanel.add(lblTotalClientsTitle);
        
        lblTotalClients = new JLabel("0");
        lblTotalClients.setForeground(COLOR_INFO);
        rightPanel.add(lblTotalClients);
        
        // Kết hợp vào panel điều khiển chính
        controlPanel.add(leftPanel, BorderLayout.WEST);
        controlPanel.add(rightPanel, BorderLayout.CENTER);
        
        return controlPanel;
    }
    
    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BACKGROUND);
        
        // Các nút chức năng
        JPanel functionPanel = new JPanel();
        functionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Chức năng", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD));
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
        btnDeleteData.setForeground(COLOR_ERROR);
        btnDeleteData.setEnabled(false);
        
        functionPanel.add(btnViewUsers);
        functionPanel.add(btnViewMessages);
        functionPanel.add(btnExportData);
        functionPanel.add(btnDeleteData);
        
        // Panel log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Lịch sử hoạt động", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD));
        logPanel.setBackground(COLOR_BACKGROUND);
        
        taLog = new JTextArea();
        taLog.setEditable(false);
        taLog.setLineWrap(true);
        taLog.setWrapStyleWord(true);
        taLog.setFont(FONT_MONOSPACE);
        taLog.setBackground(new Color(252, 252, 252));
        
        // Thiết lập tự động cuộn xuống
        DefaultCaret caret = (DefaultCaret) taLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane scrollPane = new JScrollPane(taLog);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Thêm filter log
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
        
        // Thêm sự kiện lọc
        btnFilter.addActionListener(e -> {
            String filterText = tfFilter.getText().trim();
            if (!filterText.isEmpty()) {
                filterLog(filterText);
            }
        });
        
        btnClearFilter.addActionListener(e -> {
            tfFilter.setText("");
            // Tải lại toàn bộ log
        });
        
        // Kết hợp vào panel chính
        panel.add(functionPanel, BorderLayout.NORTH);
        panel.add(logPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void filterLog(String filterText) {
        // Tạo một cửa sổ mới để hiển thị kết quả lọc
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
        
        // Lọc và hiển thị các dòng phù hợp
        String[] lines = taLog.getText().split("\n");
        int matchCount = 0;
        
        for (String line : lines) {
            if (line.toLowerCase().contains(filterText.toLowerCase())) {
                filteredLog.append(line + "\n");
                matchCount++;
            }
        }
        
        // Thêm thông tin về kết quả lọc
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblInfo = new JLabel("Tìm thấy " + matchCount + " dòng phù hợp");
        infoPanel.add(lblInfo);
        panel.add(infoPanel, BorderLayout.NORTH);
        
        // Thêm nút để xuất kết quả lọc
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
        
        // Tên file mặc định
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
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BACKGROUND);
        
        // Panel tổng quan
        JPanel overviewPanel = new JPanel(new GridLayout(1, 3, 10, 5));
        overviewPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Tổng quan", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD));
        overviewPanel.setBackground(COLOR_BACKGROUND);
        
        // Panel trạng thái cơ sở dữ liệu
        JPanel dbPanel = new JPanel(new BorderLayout());
        dbPanel.setBorder(BorderFactory.createTitledBorder("Cơ sở dữ liệu"));
        dbPanel.setBackground(COLOR_BACKGROUND_ALT);
        lblDatabaseStatus = new JLabel("Chưa kết nối");
        lblDatabaseStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblDatabaseStatus.setForeground(COLOR_WARNING);
        lblDatabaseStatus.setFont(FONT_BOLD);
        dbPanel.add(lblDatabaseStatus, BorderLayout.CENTER);
        
        // Panel số tin nhắn
        JPanel messagesPanel = new JPanel(new BorderLayout());
        messagesPanel.setBorder(BorderFactory.createTitledBorder("Số tin nhắn"));
        messagesPanel.setBackground(COLOR_BACKGROUND_ALT);
        lblMessagesCount = new JLabel("0");
        lblMessagesCount.setHorizontalAlignment(SwingConstants.CENTER);
        lblMessagesCount.setForeground(COLOR_INFO);
        lblMessagesCount.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        messagesPanel.add(lblMessagesCount, BorderLayout.CENTER);
        
        // Panel số người dùng
        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.setBorder(BorderFactory.createTitledBorder("Số người dùng"));
        usersPanel.setBackground(COLOR_BACKGROUND_ALT);
        lblUsersCount = new JLabel("0");
        lblUsersCount.setHorizontalAlignment(SwingConstants.CENTER);
        lblUsersCount.setForeground(COLOR_INFO);
        lblUsersCount.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        usersPanel.add(lblUsersCount, BorderLayout.CENTER);
        
        overviewPanel.add(dbPanel);
        overviewPanel.add(messagesPanel);
        overviewPanel.add(usersPanel);
        
        // Chart Panel cho biểu đồ hoạt động
        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawChart(g);
            }
        };
        chartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Biểu đồ hoạt động", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD));
        chartPanel.setBackground(COLOR_BACKGROUND);
        
        // Kết hợp vào panel chính
        panel.add(overviewPanel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void drawChart(Graphics g) {
        if (chartData.isEmpty()) {
            // Vẽ placeholder nếu không có dữ liệu
            g.setColor(Color.GRAY);
            g.setFont(FONT_NORMAL);
            String message = "Biểu đồ hoạt động sẽ hiển thị ở đây khi server chạy";
            FontMetrics fm = g.getFontMetrics();
            int messageWidth = fm.stringWidth(message);
            g.drawString(message, (chartPanel.getWidth() - messageWidth) / 2, chartPanel.getHeight() / 2);
            return;
        }
        
        // Thiết lập Graphics2D
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Tính toán kích thước và vị trí
        int padding = 40;
        int labelPadding = 25;
        int width = chartPanel.getWidth() - 2 * padding;
        int height = chartPanel.getHeight() - 2 * padding;
        int bottom = height + padding;
        int left = padding;
        
        // Vẽ trục X và Y
        g2d.setColor(Color.BLACK);
        g2d.drawLine(left, bottom, left + width, bottom); // Trục X
        g2d.drawLine(left, bottom, left, padding); // Trục Y
        
        // Tìm giá trị lớn nhất
        int maxValue = 1; // Tối thiểu là 1 để tránh chia cho 0
        for (int value : chartData.values()) {
            maxValue = Math.max(maxValue, value);
        }
        
        // Vẽ các mốc trên trục Y
        g2d.setFont(FONT_SMALL);
        int hatchCount = 5; // Số mốc trên trục Y
        for (int i = 0; i <= hatchCount; i++) {
            int y = bottom - (i * height / hatchCount);
            g2d.drawLine(left - 5, y, left, y); // Vẽ mốc
            
            // Vẽ nhãn
            String yLabel = String.valueOf(i * maxValue / hatchCount);
            FontMetrics metrics = g2d.getFontMetrics();
            int labelWidth = metrics.stringWidth(yLabel);
            g2d.drawString(yLabel, left - labelWidth - 8, y + metrics.getHeight() / 4);
            
            // Vẽ đường kẻ ngang
            g2d.setColor(new Color(220, 220, 220));
            g2d.drawLine(left, y, left + width, y);
            g2d.setColor(Color.BLACK);
        }
        
        // Vẽ biểu đồ
        if (chartData.size() > 1) {
            g2d.setColor(chartColor);
            g2d.setStroke(new BasicStroke(2f));
            
            // Chuyển đổi Map thành mảng để dễ truy cập
            String[] times = chartData.keySet().toArray(new String[0]);
            int[] values = new int[times.length];
            
            for (int i = 0; i < times.length; i++) {
                values[i] = chartData.get(times[i]);
            }
            
            // Vẽ đường nối các điểm
            int barWidth = width / (times.length + 1);
            
            for (int i = 0; i < times.length - 1; i++) {
                int x1 = left + (i + 1) * barWidth;
                int y1 = bottom - (int)(((double)values[i] / maxValue) * height);
                int x2 = left + (i + 2) * barWidth;
                int y2 = bottom - (int)(((double)values[i + 1] / maxValue) * height);
                
                g2d.drawLine(x1, y1, x2, y2);
                
                // Vẽ điểm
                g2d.fillOval(x1 - 3, y1 - 3, 6, 6);
            }
            
            // Vẽ điểm cuối cùng
            int lastIndex = times.length - 1;
            int x = left + (lastIndex + 1) * barWidth;
            int y = bottom - (int)(((double)values[lastIndex] / maxValue) * height);
            g2d.fillOval(x - 3, y - 3, 6, 6);
            
            // Vẽ nhãn trục X
            g2d.setColor(Color.BLACK);
            g2d.setFont(FONT_SMALL);
            
            // Chỉ vẽ một số nhãn nếu có quá nhiều
            int skipFactor = Math.max(1, times.length / 8);
            
            for (int i = 0; i < times.length; i += skipFactor) {
                String time = times[i];
                int x1 = left + (i + 1) * barWidth;
                
                // Vẽ mốc trên trục X
                g2d.drawLine(x1, bottom, x1, bottom + 5);
                
                // Vẽ nhãn thời gian
                FontMetrics metrics = g2d.getFontMetrics();
                int labelWidth = metrics.stringWidth(time);
                
                // Xoay văn bản nếu quá dài
                if (times.length > 10) {
                    g2d.rotate(-Math.PI / 4, x1, bottom + 8);
                    g2d.drawString(time, x1 - labelWidth / 2, bottom + 18);
                    g2d.rotate(Math.PI / 4, x1, bottom + 8);
                } else {
                    g2d.drawString(time, x1 - labelWidth / 2, bottom + 18);
                }
            }
        }
        
        // Vẽ chú thích
        g2d.setColor(Color.BLACK);
        g2d.setFont(FONT_NORMAL);
        String title = "Số tin nhắn theo thời gian";
        FontMetrics titleMetrics = g2d.getFontMetrics();
        g2d.drawString(title, left + (width - titleMetrics.stringWidth(title)) / 2, padding - 10);
    }
    
    private void updateChartData() {
        // Lấy thời điểm hiện tại
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String currentTime = sdf.format(new Date());
        
        // Lấy số tin nhắn hiện tại
        try {
            int messagesCount = server.getDatabaseManager().getMessagesCountByPort(server.getServerPort());
            
            // Nếu có quá nhiều điểm dữ liệu, loại bỏ điểm cũ nhất
            if (chartData.size() > 15) {
                String oldestKey = chartData.keySet().iterator().next();
                chartData.remove(oldestKey);
            }
            
            // Thêm dữ liệu mới
            chartData.put(currentTime, messagesCount);
            
            // Yêu cầu vẽ lại biểu đồ
            if (chartPanel != null) {
                chartPanel.repaint();
            }
            
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Không thể cập nhật dữ liệu biểu đồ", ex);
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
                TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD));
        
        // Cài đặt giữ log
        settingsPanel.add(new JLabel("Tự động xóa log cũ:"));
        JCheckBox cbAutoCleanLog = new JCheckBox("Bật");
        settingsPanel.add(cbAutoCleanLog);
        
        // Cài đặt số lượng tin nhắn tối đa
        settingsPanel.add(new JLabel("Số lượng tin nhắn lưu trữ tối đa:"));
        JTextField tfMaxMessages = new JTextField("1000");
        settingsPanel.add(tfMaxMessages);
        
        // Cài đặt tự động sao lưu
        settingsPanel.add(new JLabel("Tự động sao lưu dữ liệu:"));
        JCheckBox cbAutoBackup = new JCheckBox("Bật");
        settingsPanel.add(cbAutoBackup);
        
        // Cài đặt thời gian sao lưu
        settingsPanel.add(new JLabel("Thời gian giữa các lần sao lưu (phút):"));
        JTextField tfBackupInterval = new JTextField("60");
        settingsPanel.add(tfBackupInterval);
        
        // Cài đặt màu biểu đồ
        settingsPanel.add(new JLabel("Màu biểu đồ:"));
        JButton btnChangeColor = new JButton("Chọn màu");
        btnChangeColor.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Chọn màu biểu đồ", chartColor);
            if (newColor != null) {
                chartColor = newColor;
                if (chartPanel != null) {
                    chartPanel.repaint();
                }
            }
        });
        settingsPanel.add(btnChangeColor);
        
        // Cài đặt đường dẫn lưu file
        settingsPanel.add(new JLabel("Thư mục xuất dữ liệu mặc định:"));
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.setBackground(COLOR_BACKGROUND);
        JTextField tfExportPath = new JTextField("1.database");
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
        
        // Nút lưu cài đặt
        JButton btnSaveSettings = new JButton("Lưu cài đặt");
        btnSaveSettings.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Đã lưu cài đặt thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        
        // Panel nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(COLOR_BACKGROUND);
        buttonPanel.add(btnSaveSettings);
        
        panel.add(settingsPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createAboutPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BACKGROUND);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Logo hoặc ảnh
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(COLOR_BACKGROUND);
        // Nếu có logo, thêm vào đây
        JLabel lblLogo = new JLabel("CHAT SERVER", SwingConstants.CENTER);
        lblLogo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
        lblLogo.setForeground(new Color(0, 102, 204));
        logoPanel.add(lblLogo, BorderLayout.CENTER);
        
        // Thông tin ứng dụng
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(COLOR_BACKGROUND);
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin ứng dụng", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD));
        
        JTextArea taInfo = new JTextArea();
        taInfo.setEditable(false);
        taInfo.setLineWrap(true);
        taInfo.setWrapStyleWord(true);
        taInfo.setFont(FONT_NORMAL);
        taInfo.setBackground(COLOR_BACKGROUND);
        taInfo.setText(
            "Chat Server Monitor\n\n" +
            "Phiên bản: 1.0.0\n" +
            "Ngày phát hành: 04/04/2025\n\n" +
            "Ứng dụng Chat Server là một hệ thống cho phép nhiều người dùng kết nối và trao đổi tin nhắn. " +
            "Server đóng vai trò trung gian, quản lý kết nối, lưu trữ và chuyển tiếp tin nhắn giữa các client.\n\n" +
            "Tính năng chính:\n" +
            "- Hỗ trợ nhiều kết nối client đồng thời\n" +
            "- Lưu trữ tin nhắn trong cơ sở dữ liệu SQLite\n" +
            "- Hiển thị thống kê về người dùng và tin nhắn\n" +
            "- Xuất dữ liệu ra file văn bản\n" +
            "- Hỗ trợ tìm kiếm và lọc tin nhắn\n\n" +
            "Phát triển bởi: 52300262 - Phạm Hoài Thương\n"
        );
        
        JScrollPane scrollPane = new JScrollPane(taInfo);
        scrollPane.setBorder(null);
        infoPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Phiên bản Java và hệ thống
        JPanel systemPanel = new JPanel(new BorderLayout());
        systemPanel.setBackground(COLOR_BACKGROUND);
        systemPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin hệ thống", 
                TitledBorder.LEFT, TitledBorder.TOP, FONT_BOLD));
        
        JTextArea taSystem = new JTextArea();
        taSystem.setEditable(false);
        taSystem.setLineWrap(true);
        taSystem.setWrapStyleWord(true);
        taSystem.setFont(FONT_NORMAL);
        taSystem.setBackground(COLOR_BACKGROUND);
        
        // Lấy thông tin hệ thống
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        
        taSystem.setText(
            "Java: " + javaVersion + "\n" +
            "Hệ điều hành: " + osName + " " + osVersion + "\n" +
            "Kiến trúc: " + osArch + "\n" +
            "SQLite JDBC: 349.1.0\n"
        );
        
        systemPanel.add(taSystem, BorderLayout.CENTER);
        
        // Kết hợp các panel
        panel.add(logoPanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(systemPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.setBackground(new Color(240, 240, 240));
        
        // Thời gian chạy server
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(null);
        lblServerUptime = new JLabel("Uptime: 00:00:00");
        lblServerUptime.setFont(FONT_SMALL);
        leftPanel.add(lblServerUptime);
        
        // Thông tin sử dụng tài nguyên
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(null);
        
        lblServerMemory = new JLabel("RAM: 0 MB");
        lblServerMemory.setFont(FONT_SMALL);
        
        lblServerCPU = new JLabel("CPU: 0%");
        lblServerCPU.setFont(FONT_SMALL);
        
        JLabel lblVersion = new JLabel("v1.0.0");
        lblVersion.setFont(FONT_SMALL);
        
        rightPanel.add(lblServerMemory);
        rightPanel.add(lblServerCPU);
        rightPanel.add(lblVersion);
        
        statusBar.add(leftPanel, BorderLayout.WEST);
        statusBar.add(rightPanel, BorderLayout.EAST);
        
        return statusBar;
    }
    
    private void setupEventHandlers() {
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });
        
        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });
        
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                taLog.setText("");
            }
        });
        
        btnViewUsers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showUsersDialog();
            }
        });
        
        btnViewMessages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMessagesDialog();
            }
        });
        
        btnExportData.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportData();
            }
        });
        
        btnDeleteData.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deletePortData();
            }
        });
        
        // Tab changed event
        tabbedPane.addChangeListener(e -> {
            // Cập nhật dữ liệu khi chuyển đến tab Thống kê
            if (tabbedPane.getSelectedIndex() == 1 && server.isRunning()) {
                updateStatistics();
                updateChartData();
            }
        });
        
        // Sự kiện đóng cửa sổ
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
                
                // Dừng các timer
                uptimeTimer.stop();
                resourceMonitorTimer.stop();
                
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
            // Hiển thị dialog tiến trình
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
                    updateStatistics();
                    updateChartData();
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
        
        // Thư mục mặc định để lưu
        File defaultDir = new File("1.database");
        if (!defaultDir.exists()) {
            defaultDir.mkdir();
        }
        fileChooser.setCurrentDirectory(defaultDir);
        
        // Tên file mặc định
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
            
            // Hiển thị dialog tiến trình
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
                // Xuất dữ liệu theo port hiện tại
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
                return false; // Không cho phép sửa dữ liệu trong bảng
            }
        };
        
        model.addColumn("STT");
        model.addColumn("Tên người dùng");
        model.addColumn("Lần đăng nhập gần nhất");
        model.addColumn("Số lần kết nối");
        
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.setRowHeight(25);
        table.setFont(FONT_NORMAL);
        
        // Căn giữa các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        
        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        // Panel thông tin
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(COLOR_BACKGROUND);
        JLabel lblInfo = new JLabel("Tổng số người dùng đã từng kết nối: 0");
        lblInfo.setFont(FONT_NORMAL);
        infoPanel.add(lblInfo);
        dialog.add(infoPanel, BorderLayout.NORTH);
        
        // Panel nút
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(COLOR_BACKGROUND);
        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(FONT_NORMAL);
        JButton btnExport = new JButton("Xuất danh sách");
        btnExport.setFont(FONT_NORMAL);
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(FONT_NORMAL);
        
        btnRefresh.addActionListener(e -> {
            model.setRowCount(0); // Xóa dữ liệu cũ
            loadUsersData(model, lblInfo); // Tải lại dữ liệu
        });
        
        btnExport.addActionListener(e -> {
            exportUsersList(table);
        });
        
        btnClose.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnExport);
        buttonPanel.add(btnClose);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Tải dữ liệu người dùng
        loadUsersData(model, lblInfo);
        
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
                
                writer.println(String.format("%-5s %-20s %-30s %-15s", "STT", "Tên người dùng", "Lần đăng nhập gần nhất", "Số lần kết nối"));
                writer.println(String.format("%-5s %-20s %-30s %-15s", "-----", "--------------------", "------------------------------", "---------------"));
                
                // Lấy dữ liệu từ bảng
                for (int i = 0; i < table.getRowCount(); i++) {
                    String stt = table.getValueAt(i, 0).toString();
                    String username = table.getValueAt(i, 1).toString();
                    String lastLogin = table.getValueAt(i, 2).toString();
                    String connectionCount = table.getValueAt(i, 3).toString();
                    
                    writer.println(String.format("%-5s %-20s %-30s %-15s", stt, username, lastLogin, connectionCount));
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
    
    private void loadUsersData(DefaultTableModel model, JLabel lblInfo) {
        try {
            ResultSet rs = server.getDatabaseManager().getActiveUsers(server.getServerPort());
            boolean hasUsers = false;
            int rowCount = 0;
            
            while (rs.next()) {
                hasUsers = true;
                rowCount++;
                String username = rs.getString("username");
                String lastLogin = rs.getString("last_login");
                int connectionCount = rs.getInt("connection_count");
                
                model.addRow(new Object[]{rowCount, username, lastLogin, connectionCount});
            }
            
            if (!hasUsers) {
                model.addRow(new Object[]{"", "Không có người dùng nào đã kết nối đến port này.", "", ""});
            }
            
            lblInfo.setText("Tổng số người dùng đã từng kết nối: " + rowCount);
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
        
        // Thêm panel cho tìm kiếm
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
        
        // Tạo model và table để hiển thị tin nhắn
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép sửa dữ liệu trong bảng
            }
        };
        
        model.addColumn("STT");
        model.addColumn("Thời gian");
        model.addColumn("Người dùng");
        model.addColumn("Tin nhắn");
        
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(400);
        table.setRowHeight(25);
        table.setFont(FONT_NORMAL);
        
        // Căn giữa cột STT
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        
        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, BorderLayout.CENTER);
        
        // Panel thông tin
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBackground(COLOR_BACKGROUND);
        JLabel lblInfo = new JLabel("Hiển thị 0/0 tin nhắn");
        lblInfo.setFont(FONT_NORMAL);
        infoPanel.add(lblInfo);
        
        // Panel nút
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
        
        // Tải dữ liệu tin nhắn
        loadMessagesData(model, lblInfo, null);
        
        // Xử lý sự kiện tìm kiếm
        ActionListener searchAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = tfSearch.getText().trim();
                loadMessagesData(model, lblInfo, searchText.isEmpty() ? null : searchText);
            }
        };
        
        btnSearch.addActionListener(searchAction);
        tfSearch.addActionListener(searchAction); // Cho phép nhấn Enter để tìm kiếm
        
        btnClearSearch.addActionListener(e -> {
            tfSearch.setText("");
            loadMessagesData(model, lblInfo, null);
        });
        
        btnRefresh.addActionListener(e -> {
            String searchText = tfSearch.getText().trim();
            loadMessagesData(model, lblInfo, searchText.isEmpty() ? null : searchText);
        });
        
        btnExport.addActionListener(e -> {
            String searchText = tfSearch.getText().trim();
            exportFilteredMessages(searchText);
        });
        
        btnClose.addActionListener(e -> dialog.dispose());
        
        dialog.setVisible(true);
    }
    
    private void loadMessagesData(DefaultTableModel model, JLabel lblInfo, String searchText) {
        model.setRowCount(0); // Xóa dữ liệu cũ
        
        try {
            ResultSet rs;
            int totalMessages = server.getDatabaseManager().getMessagesCountByPort(server.getServerPort());
            
            if (searchText == null || searchText.isEmpty()) {
                rs = server.getDatabaseManager().getRecentMessages(1000, server.getServerPort());
            } else {
                rs = server.getDatabaseManager().searchMessages(searchText, server.getServerPort());
            }
            
            boolean hasMessages = false;
            int rowCount = 0;
            
            while (rs.next()) {
                hasMessages = true;
                rowCount++;
                String username = rs.getString("username");
                String message = rs.getString("message");
                String timestamp = rs.getString("timestamp");
                
                model.addRow(new Object[]{rowCount, timestamp, username, message});
            }
            
            if (!hasMessages) {
                if (searchText != null && !searchText.isEmpty()) {
                    model.addRow(new Object[]{"", "", "Không tìm thấy tin nhắn phù hợp.", ""});
                } else {
                    model.addRow(new Object[]{"", "", "Chưa có tin nhắn nào trên port này.", ""});
                }
                lblInfo.setText("Hiển thị 0/" + totalMessages + " tin nhắn");
            } else {
                String displayText = "Hiển thị " + rowCount;
                if (searchText != null && !searchText.isEmpty()) {
                    displayText += " kết quả tìm kiếm";
                } else {
                    displayText += "/" + totalMessages;
                }
                displayText += " tin nhắn";
                lblInfo.setText(displayText);
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
        
        // Thư mục mặc định để lưu
        File defaultDir = new File("1.database");
        if (!defaultDir.exists()) {
            defaultDir.mkdir();
        }
        fileChooser.setCurrentDirectory(defaultDir);
        
        // Tên file mặc định
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
            
            // Hiển thị dialog tiến trình
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
                // Xuất tin nhắn đã lọc
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
            
            // Hiển thị thanh tiến trình khi khởi động
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
            
            // Tạo luồng để khởi động server
            new Thread(() -> {
                boolean success = server.start(port);
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    
                    if (success) {
                        startTime = new Date();
                        updateUIOnStartSuccess();
                        updateStatistics();
                        
                        // Khởi tạo dữ liệu biểu đồ
                        chartData.clear();
                        updateChartData();
                        
                        // Khởi động timer
                        uptimeTimer.start();
                        resourceMonitorTimer.start();
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
        lblDatabaseStatus.setText("Đã kết nối");
        lblDatabaseStatus.setForeground(COLOR_SUCCESS);
        isDatabaseConnected = true;
        
        updateClientCount(0);
        updateTotalClients(0);
        
        // Cập nhật tiêu đề
        setTitle("Chat Server Monitor - Port: " + server.getServerPort());
        
        // Chuyển đến tab Console
        tabbedPane.setSelectedIndex(0);
        
        // Cập nhật uptime
        updateUptimeLabel();
        
        // Cập nhật thông tin sử dụng tài nguyên
        updateResourceUsage();
    }
    
    private void updateUptimeLabel() {
        if (startTime != null) {
            long diff = new Date().getTime() - startTime.getTime();
            long seconds = diff / 1000 % 60;
            long minutes = diff / (60 * 1000) % 60;
            long hours = diff / (60 * 60 * 1000) % 24;
            long days = diff / (24 * 60 * 60 * 1000);
            
            String uptime;
            if (days > 0) {
                uptime = String.format("Uptime: %d days, %02d:%02d:%02d", days, hours, minutes, seconds);
            } else {
                uptime = String.format("Uptime: %02d:%02d:%02d", hours, minutes, seconds);
            }
            
            lblServerUptime.setText(uptime);
        }
    }
    
    private void updateResourceUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        // Tính toán sử dụng bộ nhớ
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Hiển thị sử dụng RAM (MB)
        lblServerMemory.setText(String.format("RAM: %.1f MB", usedMemory / (1024.0 * 1024.0)));
        
        // Giả lập sử dụng CPU (trong thực tế, sẽ cần cách khác để đo chính xác)
        double cpuLoad = Math.random() * 10; // Giả lập 0-10%
        lblServerCPU.setText(String.format("CPU: %.1f%%", cpuLoad));
    }
    
    private void updateStatistics() {
        if (server.isRunning()) {
            try {
                // Cập nhật số liệu thống kê
                int messagesCount = server.getDatabaseManager().getMessagesCountByPort(server.getServerPort());
                int usersCount = server.getDatabaseManager().getUsersCountByPort(server.getServerPort());
                
                lblMessagesCount.setText(String.valueOf(messagesCount));
                lblUsersCount.setText(String.valueOf(usersCount));
                
                // Cập nhật dữ liệu biểu đồ định kỳ (mỗi phút)
                Calendar now = Calendar.getInstance();
                if (now.get(Calendar.SECOND) == 0) {
                    updateChartData();
                }
                
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Không thể cập nhật thống kê", ex);
            }
        }
    }
    
    private void stopServer() {
        // Hiển thị hộp thoại xác nhận
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc chắn muốn dừng server?\n" +
                "Tất cả client đang kết nối sẽ bị ngắt kết nối.",
                "Xác nhận dừng server", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Hiển thị thanh tiến trình khi dừng server
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
            
            // Tạo luồng để dừng server
            new Thread(() -> {
                server.stop();
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    
                    // Dừng các timer
                    uptimeTimer.stop();
                    resourceMonitorTimer.stop();
                    
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
        lblDatabaseStatus.setText("Đã ngắt kết nối");
        lblDatabaseStatus.setForeground(COLOR_WARNING);
        isDatabaseConnected = false;
        
        updateClientCount(0);
        setTitle("Chat Server Monitor");
        
        // Reset số liệu thống kê
        lblMessagesCount.setText("0");
        lblUsersCount.setText("0");
        
        // Reset thông tin uptime và tài nguyên
        lblServerUptime.setText("Uptime: 00:00:00");
        lblServerMemory.setText("RAM: 0 MB");
        lblServerCPU.setText("CPU: 0%");
        
        // Thêm dòng log
        logMessage("Server đã dừng trên port " + server.getServerPort());
    }
    
    public void logMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timeStamp = sdf.format(new Date());
                taLog.append("[" + timeStamp + "] " + message + "\n");
                
                // Nếu log nói về tin nhắn mới, cập nhật thống kê
                if (message.contains(":") && !message.startsWith("[") && isDatabaseConnected) {
                    updateStatistics();
                }
            }
        });
    }
    
    public void updateClientCount(int count) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                lblClientCount.setText(String.valueOf(count));
                
                // Cập nhật tiêu đề nếu server đang chạy
                if (server.isRunning()) {
                    setTitle("Chat Server Monitor - Port: " + server.getServerPort() + " - Clients: " + count);
                }
            }
        });
    }
    
    public void updateTotalClients(int count) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                lblTotalClients.setText(String.valueOf(count));
            }
        });
    }
    
    public static void main(String[] args) {
        try {
            // Thiết lập giao diện người dùng theo phong cách hệ thống
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Không thể thiết lập giao diện hệ thống", ex);
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatServerGUI().setVisible(true);
            }
        });
    }
}