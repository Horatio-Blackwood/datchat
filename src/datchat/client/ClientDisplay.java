package datchat.client;

import datchat.UserStatus;
import datchat.ChatMessage;
import datchat.Datchat;
import datchat.MessageType;
import datchat.OnlineStatus;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.metal.MetalLookAndFeel;


/**
 * The Client Display.
 *
 * @author victor
 * @author adam
 */
public class ClientDisplay {

    /** SVID */
    private static final long serialVersionUID = 1L;

    /** Text Field for entering a username. */
    private final JTextField m_usernameField;

    /** The Text Field for Chat message entry. */
    private JTextField m_chatTextField;

    /** Text field for entering a server host. */
    private final JTextField m_serverHostField;

    /** Text field for entering a server port. */
    private final JTextField m_serverPortField;

    /** A button for logging in or out. */
    private JButton m_loginoutButton;

    /** The area where instant messages show up. */
    private final JTextArea m_chatArea;
    
    /** The list of users currently online. */
    private SortableListModel<UserStatus> m_userListModel;

    /** */
    private boolean m_connected;

    /** The top-level window of this app. */
    private final JFrame m_frame;

    /** A list of client display listeners (probably will always be only one in this list). */
    private final List<ClientDisplayListener> m_listeners;

    /** An action listener for sending messages when a user hits enter. */
    private ActionListener m_textFieldActionListener;

    /** Swing buttons are fat, this dimension makes them not fat. */
    private static final Dimension BUTTON_DIM = new Dimension(120, 20);
    
    private static final Font FONT = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font BOLD = new Font("Monospaced", Font.BOLD, 13);

    /**
     * Constructor.
     * @param host the default server hostname (or ip).
     * @param port the default server port.
     * @param user the default username to pre-populate the username box with.
     */
    public ClientDisplay(String host, int port, String user) {
        m_listeners = new ArrayList<>();

        // CREATE COMPONENTS FOR SERVER CONFIGURATION PANEL
        // Username
        m_usernameField = new JTextField(15);
        m_usernameField.setDocument(new LimitedLengthDocument(Datchat.MAX_USERNAME_CHARS));
        m_usernameField.setText(user);
        m_usernameField.setFont(FONT);
        
        // Host
        m_serverHostField = new JTextField(15);
        m_serverHostField.setText(host);
        m_serverHostField.setFont(FONT);
        
        // Port
        m_serverPortField = new JTextField(15);
        m_serverPortField.setText(String.valueOf(port));
        m_serverPortField.setFont(FONT);
        initButtons();


        // COLUMN 0
        JPanel serverConfigPanel = new JPanel(new GridBagLayout());
        serverConfigPanel.setBorder(BorderFactory.createEtchedBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 2, 2, 2);
        JLabel srvLabel = new JLabel("Server:");
        srvLabel.setHorizontalAlignment(JLabel.RIGHT);
        srvLabel.setFont(FONT);
        serverConfigPanel.add(srvLabel, gbc);

        gbc.gridy = 1;
        JLabel portLabel =new JLabel("Port:");
        portLabel.setHorizontalAlignment(JLabel.RIGHT);
        portLabel.setFont(FONT);
        serverConfigPanel.add(portLabel, gbc);

        gbc.gridy = 2;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setHorizontalAlignment(JLabel.RIGHT);
        userLabel.setFont(FONT);
        serverConfigPanel.add(userLabel, gbc);

        // COLUMN 1
        gbc.gridx = 1;
        gbc.gridy = 0;
        serverConfigPanel.add(m_serverHostField, gbc);

        gbc.gridy = 1;
        serverConfigPanel.add(m_serverPortField, gbc);

        gbc.gridy = 2;
        serverConfigPanel.add(m_usernameField, gbc);

        // COLUMN 2;
        gbc.gridx = 2;
        gbc.gridy = 0;
        serverConfigPanel.add(m_loginoutButton, gbc);


        // CONFIGURE COMPONENTS FOR CHAT ROOM TEXT AREA
        JPanel chatArea = new JPanel(new BorderLayout());
        m_chatArea = new JTextArea("//datchat area\n", 20, 20);
        m_chatArea.setWrapStyleWord(true);
        m_chatArea.setLineWrap(true);
        m_chatArea.setFont(FONT);
        m_chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(m_chatArea);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // CREATE CONTROL PANEL (input area)
        JPanel controls = createControlPanel();
        chatArea.add(serverConfigPanel, BorderLayout.NORTH);
        chatArea.add(chatScrollPane, BorderLayout.CENTER);
        chatArea.add(controls, BorderLayout.SOUTH);
        
        // CREATE USERLIST PANEL
        JPanel userlist = createUserList();
        JScrollPane userScrollPane = new JScrollPane(userlist);
        userScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        userScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // CONFIGURE JFRAME
        m_frame = new JFrame("DatChat Client - " + Datchat.VERSION);
        m_frame.setLayout(new BorderLayout());
        m_frame.add(chatArea, BorderLayout.CENTER);
        m_frame.add(userScrollPane, BorderLayout.EAST);
        
        // Alert user if default screen name is too long.
        if (user.length() > Datchat.MAX_USERNAME_CHARS) {
            showMessage("Default username '" + user + "' is longer than max username length of " + Datchat.MAX_USERNAME_CHARS + " characters.");
        }
    }

    public void launch() {
        m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        m_frame.setSize(720, 520);
        m_frame.setMinimumSize(new Dimension(720, 520));
        m_frame.setLocationRelativeTo(null);
        m_frame.setVisible(true);
        m_usernameField.requestFocus();
    }

    /**
     * Adds the supplied listener to the display.
     * @param cdl the listener to add.
     */
    public void addListener(ClientDisplayListener cdl) {
        if (cdl != null) {
            m_listeners.add(cdl);
        }
    }

    /** Cheesy helper method to initialize the buttons and their actions. */
    private void initButtons() {
        m_loginoutButton = new JButton("Login");
        m_loginoutButton.setMinimumSize(BUTTON_DIM);
        m_loginoutButton.setMaximumSize(BUTTON_DIM);
        m_loginoutButton.setPreferredSize(BUTTON_DIM);
        m_loginoutButton.setFont(BOLD);
        m_loginoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (m_connected) {
                    showMessage("Disconnecting...");
                    // Log out
                    for (ClientDisplayListener cdl : m_listeners) {
                        cdl.sendMessage(new ChatMessage(MessageType.LOGOUT, ""));
                    }
                    m_loginoutButton.setText("Login");
                } else {
                    // Log in
                    showMessage("Connecting...");
                    String username = m_usernameField.getText().trim();
                    if (username.isEmpty()) {
                        showMessage(" - Failed to connect.  Can't log in with empty screen name.");
                        connectionFailed();
                        return;
                    }
                    String server = m_serverHostField.getText().trim();
                    if (server.isEmpty()) {
                        showMessage(" - Failed to connect.  Server must be specified.");
                        connectionFailed();
                        return;
                    }
                    String portNumber = m_serverPortField.getText().trim();
                    if (portNumber.isEmpty()) {
                        showMessage(" - Failed to connect.  Server port must be specified.");
                        connectionFailed();
                        return;
                    }

                    int port = 0;
                    try {
                        port = Integer.parseInt(portNumber);
                    } catch (NumberFormatException nfe) {
                        showMessage(" - Failed to connect.  Server port must be an integer.");
                        connectionFailed();
                        return;
                    }

                    for (ClientDisplayListener cdl : m_listeners) {
                        boolean connected = cdl.connect(server, port, username);
                        if (!connected) {
                            System.out.println(" - Failed to connect.");
                            connectionFailed();
                            return;
                        }
                        showMessage(" - Client connected.");
                    }
                    connectionEstablished();
                }
            }
        });
    }
    
    private JPanel createUserList() {
        // Build Panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(150, 150));
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        // Build Contents
        m_userListModel = new SortableListModel<>(true);
        JList<UserStatus> userList = new JList<>(m_userListModel);
        userList.setCellRenderer((JList<? extends UserStatus> jlist, UserStatus e, int i, boolean isSelected, boolean cellHasFocus) -> {
            JLabel cell = new JLabel(e.user);
            cell.setFont(FONT);
            cell.setHorizontalAlignment(JLabel.LEFT);
            cell.setToolTipText("connected since " + Datchat.CHAT_DATE_FORMATTER.format(e.sinceTime) + " on host " + e.hostname + ".");
            
            Color deselectedBackground = cell.getBackground();
            Color deselectedTextColor = cell.getForeground();
            
            if (isSelected){
                cell.setOpaque(true);
                cell.setBackground(MetalLookAndFeel.getTextHighlightColor());
            } else {
                cell.setBackground(deselectedBackground);
                cell.setForeground(deselectedTextColor);
            }

            return cell;
        });
        
        // Set contents and return panel
        JLabel title = new JLabel("Who's on Datchat?");
        panel.add(title, BorderLayout.NORTH);
        panel.add(userList, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        // TextField Action listener for when the user hits enter to send an IM.
        m_textFieldActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (m_connected) {
                    for (ClientDisplayListener cdl : m_listeners) {
                        cdl.sendMessage(new ChatMessage(MessageType.MESSAGE, m_chatTextField.getText()));
                    }
                    m_chatTextField.setText("");
                }
            }
        };
        m_chatTextField = new JTextField(20);
        m_chatTextField.setFont(FONT);
        m_chatTextField.addActionListener(m_textFieldActionListener);

        controlPanel.add(m_chatTextField, BorderLayout.CENTER);
        return controlPanel;
    }

    /**
     * Shows the new text in the chat text area.  This method automatically appends a new line.
     * @param msg the message to show.
     */
    public void showMessage(String msg) {
        m_chatArea.append(msg);
        m_chatArea.append(System.lineSeparator());
        SwingUtilities.invokeLater(() -> {
            m_chatArea.setCaretPosition(m_chatArea.getText().length() - 1);
        });
    }
    
    void updateUserStatus(UserStatus userStat) {
        // Remove the old status...
        for (UserStatus us : m_userListModel.getAllElements()) {
            if (us.isSameUser(userStat.user, userStat.hostname)) {
                m_userListModel.removeElement(us);
                break;
            }
        }
        
        // If they're online (not offline) re-add them.
        if (userStat.status == OnlineStatus.ONLINE) {
            m_userListModel.addElement(userStat);
        }
    }


    public void connectionFailed() {
        // Re-Enable Login, Username and Server.
        m_usernameField.setEnabled(true);
        m_serverHostField.setEnabled(true);

        // Re-Enable Server Config Fields
        m_serverHostField.setEditable(true);
        m_serverPortField.setEditable(true);

        // Disable Connected-Only Functions
        m_chatTextField.setEnabled(false);

        // Set connected state flag.
        m_connected = false;
        m_loginoutButton.setText("Login");
        
        // Clear Userlist
        m_userListModel.clear();
    }

    public void connectionEstablished() {
        m_connected = true;
        m_loginoutButton.setText("Logout");

        // disable login button and username field
        m_usernameField.setEnabled(false);
        m_serverHostField.setEnabled(false);

        // disable the server and port JTextField
        m_serverHostField.setEditable(false);
        m_serverPortField.setEditable(false);

        // enable log out and whos online buttons.
        m_chatTextField.setEnabled(true);
    }
}
