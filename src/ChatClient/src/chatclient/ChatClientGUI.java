package chatclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class ChatClientGUI extends JFrame {
    private JTextField tfServerIP, tfPort, tfUsername, tfMessage;
    private JTextArea taChat;
    private JButton btnConnect, btnDisconnect, btnNew, btnSend;
    private JPanel connectionPanel, chatPanel;
    private ChatClient client;
    private boolean isConnected = false;
    private static int clientCounter = 0;
    private static final Logger logger = Logger.getLogger(ChatClientGUI.class.getName());

    public ChatClientGUI() {
        client = new ChatClient(this);
        clientCounter++;
        initComponents();
        setupLogger();
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

    private void initComponents() {
        // Thiết lập cơ bản
        setTitle("Chat Client #" + clientCounter);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null);

        // Panel kết nối
        connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Thông tin kết nối"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        tfUsername = new JTextField("User" + clientCounter, 15);
        connectionPanel.add(tfUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        connectionPanel.add(new JLabel("Server IP:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        tfServerIP = new JTextField("localhost", 15);
        connectionPanel.add(tfServerIP, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        connectionPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        tfPort = new JTextField("12345", 15);
        connectionPanel.add(tfPort, gbc);

        // Panel cho các nút
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        btnConnect = new JButton("Kết nối");
        btnDisconnect = new JButton("Ngắt kết nối");
        btnNew = new JButton("Tạo cửa sổ mới");
        
        btnDisconnect.setEnabled(false);
        
        buttonPanel.add(btnConnect);
        buttonPanel.add(btnDisconnect);
        buttonPanel.add(btnNew);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        connectionPanel.add(buttonPanel, gbc);

        // Panel chat
        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Tin nhắn"));

        taChat = new JTextArea();
        taChat.setEditable(false);
        taChat.setLineWrap(true);
        taChat.setWrapStyleWord(true);
        taChat.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(taChat);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout());
        tfMessage = new JTextField();
        tfMessage.setEnabled(false);
        
        btnSend = new JButton("Gửi");
        btnSend.setEnabled(false);
        
        messagePanel.add(tfMessage, BorderLayout.CENTER);
        messagePanel.add(btnSend, BorderLayout.EAST);
        chatPanel.add(messagePanel, BorderLayout.SOUTH);

        // Layout chính
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(connectionPanel, BorderLayout.NORTH);
        getContentPane().add(chatPanel, BorderLayout.CENTER);

        // Sự kiện
        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });

        btnDisconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnectFromServer();
            }
        });
        
        btnNew.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openNewChatWindow();
            }
        });
        
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        tfMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

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

    private void connectToServer() {
        String serverIP = tfServerIP.getText().trim();
        String portText = tfPort.getText().trim();
        String username = tfUsername.getText().trim();

        if (serverIP.isEmpty() || portText.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin kết nối!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            if (client.connect(serverIP, port, username)) {
                isConnected = true;
                updateUIOnConnect();
                logger.info("Đã kết nối đến server " + serverIP + " port " + port);
                displayMessage("Đã kết nối đến server " + serverIP + " qua port " + port + "!");
            } else {
                JOptionPane.showMessageDialog(this, "Không thể kết nối đến server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnectFromServer() {
        if (isConnected) {
            client.disconnect();
            updateUIOnDisconnect();
            
            String serverIP = tfServerIP.getText().trim();
            String portText = tfPort.getText().trim();
            logger.info("Đã ngắt kết nối từ server " + serverIP + " port " + portText);
            displayMessage("Đã ngắt kết nối từ server " + serverIP + " port " + portText + "!");
        }
    }
    
    public void handleServerShutdown() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    updateUIOnDisconnect();
                    logger.info("Server đã đóng kết nối");
                    displayMessage("Server đã đóng. Bạn đã bị ngắt kết nối.");
                    JOptionPane.showMessageDialog(ChatClientGUI.this, 
                            "Server đã đóng kết nối!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
    }
    
    private void updateUIOnConnect() {
        tfServerIP.setEnabled(false);
        tfPort.setEnabled(false);
        tfUsername.setEnabled(false);
        btnConnect.setEnabled(false);
        btnDisconnect.setEnabled(true);
        tfMessage.setEnabled(true);
        btnSend.setEnabled(true);
        
        // Cập nhật tiêu đề
        String username = tfUsername.getText().trim();
        setTitle("Chat Client - " + username);
    }
    
    private void updateUIOnDisconnect() {
        isConnected = false;
        btnDisconnect.setEnabled(false);
        btnConnect.setEnabled(true);
        tfMessage.setEnabled(false);
        btnSend.setEnabled(false);
        tfServerIP.setEnabled(true);
        tfPort.setEnabled(true);
        tfUsername.setEnabled(true);
        
        // Cập nhật tiêu đề trở lại
        setTitle("Chat Client #" + clientCounter);
    }
    
    private void openNewChatWindow() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClientGUI().setVisible(true);
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

    public void displayMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String timeStamp = sdf.format(new Date());
                taChat.append("[" + timeStamp + "] " + message + "\n");
                // Cuộn xuống cuối
                taChat.setCaretPosition(taChat.getDocument().getLength());
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClientGUI().setVisible(true);
            }
        });
    }
}