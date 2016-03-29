package datchat.client;

import datchat.ChatMessage;
import datchat.Datchat;
import datchat.MessageType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


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
    
    /** */
    private final JTextField m_serverHostField;
    
    /** */
    private final JTextField m_serverPortField;
    
    /** */
    private JButton m_loginButton;
    
    /** */
    private JButton m_logoutButton;
    
    /** */
    private JButton m_whosOnlineButton;
    
    /** The area where instant messages show up. */
    private final JTextArea m_chatArea;
    
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

    /**
     * Constructor.
     * @param host the server's hostname.
     * @param port the server's port.
     */
    public ClientDisplay(String host, int port) {
        m_listeners = new ArrayList<>();
        
        // CREATE COMPONENTS FOR SERVER CONFIGURATION PANEL
        m_usernameField = new JTextField(10);
        m_usernameField.setText("<user>");
        m_serverHostField = new JTextField(10);
        m_serverHostField.setText(host);
        m_serverPortField = new JTextField(10);
        m_serverPortField.setText(String.valueOf(port));
        initButtons();
        
        
        // COLUMN 0
        JPanel serverConfigPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 2, 2, 2);
        serverConfigPanel.add(new JLabel("Server:"), gbc);
        
        gbc.gridy = 1;
        serverConfigPanel.add(new JLabel("Port:"), gbc);
        
        gbc.gridy = 2;
        serverConfigPanel.add(new JLabel("Username:"), gbc);
        
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
        serverConfigPanel.add(m_loginButton, gbc);
        
        gbc.gridy = 1;
        serverConfigPanel.add(m_logoutButton, gbc);
        
        gbc.gridy = 2;
        serverConfigPanel.add(m_whosOnlineButton, gbc);
        

        // CONFIGURE COMPONENTS FOR CHAT ROOM TEXT AREA
        m_chatArea = new JTextArea("//datchat area\n", 20, 20);
        m_chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        m_chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(m_chatArea);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);        
        
        // CREATE CONTROL PANEL
        JPanel controls = createControlPanel();
        
        // CONFIGURE JFRAME
        m_frame = new JFrame("DatChat Client - " + Datchat.VERSION);
        m_frame.setLayout(new BorderLayout());
        m_frame.add(serverConfigPanel, BorderLayout.NORTH);
        m_frame.add(chatScrollPane, BorderLayout.CENTER);
        m_frame.add(controls, BorderLayout.SOUTH);
    }
    
    public void launch() {
        m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        m_frame.setSize(640, 480);
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
    
    /** Cheesey helper method to initialize the buttons and their actions. */
    private void initButtons() {
        m_loginButton = new JButton("Login");
        m_loginButton.setMinimumSize(BUTTON_DIM);
        m_loginButton.setMaximumSize(BUTTON_DIM);
        m_loginButton.setPreferredSize(BUTTON_DIM);
        m_loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                System.out.println("logging in.");
                
                String username = m_usernameField.getText().trim();
                if (username.isEmpty()) {
                    showMessage("Can't log in with empty screen name.");
                    return;
                }
                String server = m_serverHostField.getText().trim();
                if (server.isEmpty()) {
                    showMessage("Server must be specified.");
                    return;
                }
                String portNumber = m_serverPortField.getText().trim();
                if (portNumber.isEmpty()) {
                    showMessage("Server port must be specified.");
                    return;
                }
                
                int port = 0;
                try {
                    port = Integer.parseInt(portNumber);
                } catch (NumberFormatException nfe) {
                    showMessage("Server port must be an integer.");
                    return;
                }
                
                for (ClientDisplayListener cdl : m_listeners) {
                    System.out.println("Connecting via CDL.");
                    boolean connected = cdl.connect(server, port, username);
                    if (!connected) {
                        System.out.println("Failed to connect.");
                        return;
                    }
                    System.out.println("connected!");
                }
//                client = new Client(server, port, username, ClientDisplay.this);
//                if (!client.start()) {
//                    return;
//                }

                m_connected = true;

                // disable login button and username field
                m_loginButton.setEnabled(false);
                m_usernameField.setEnabled(false);
                m_serverHostField.setEnabled(false);

                // disable the server and port JTextField
                m_serverHostField.setEditable(false);
                m_serverPortField.setEditable(false);                
                
                // enable log out and whos online buttons.
                m_logoutButton.setEnabled(true);
                m_whosOnlineButton.setEnabled(true);
                m_chatTextField.setEnabled(true);
            }
        });
                
        m_logoutButton = new JButton("Logout");
        m_logoutButton.setMinimumSize(BUTTON_DIM);
        m_logoutButton.setPreferredSize(BUTTON_DIM);
        m_logoutButton.setMaximumSize(BUTTON_DIM);
        m_logoutButton.setEnabled(false); // you have to login before being able to logout
        m_logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                for (ClientDisplayListener cdl : m_listeners) {
                    cdl.sendMessage(new ChatMessage(MessageType.LOGOUT, ""));
                }
            }
        });        
        
        m_whosOnlineButton = new JButton("Who's Online?");
        m_whosOnlineButton.setEnabled(false);
        m_whosOnlineButton.setMinimumSize(BUTTON_DIM);
        m_whosOnlineButton.setMaximumSize(BUTTON_DIM);
        m_whosOnlineButton.setPreferredSize(BUTTON_DIM);
        m_whosOnlineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                for (ClientDisplayListener cdl : m_listeners) {
                    cdl.sendMessage(new ChatMessage(MessageType.WHOS_ONLINE, ""));
                }
            }
        });  
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
        m_chatArea.setCaretPosition(m_chatArea.getText().length() - 1);
    }

    
    public void connectionFailed() {
        // Re-Enable Login, Username and Server.
        m_loginButton.setEnabled(true);
        m_usernameField.setEnabled(true);
        m_serverHostField.setEnabled(true);    

        // Re-Enable Server Config Fields
        m_serverHostField.setEditable(true);
        m_serverPortField.setEditable(true);        
        
        // Disable Connected-Only Functions
        m_logoutButton.setEnabled(false);
        m_whosOnlineButton.setEnabled(false);
        m_chatTextField.setEnabled(false);

        // Set connected state flag.
        m_connected = false;
    }
}
