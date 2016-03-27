package datchat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


/**
 *
 * @author victor
 */
public class ClientGUI {

    private static final long serialVersionUID = 1L;
    private JLabel label;
    private JTextField m_tf;
    private JTextField tfServer, tfPort;
    private JButton login, logout, whoIsIn;
    private JTextArea ta;
    private boolean connected;
    private Client client;
    private int defaultPort;
    private String defaultHost;
    private JFrame m_frame;
    
    private ActionListener m_buttonListener;

    // Constructor connection receiving socket number
    ClientGUI(String host, int port) {
        m_frame = new JFrame("DatChat Client - " + Datchat.VERSION);
        defaultPort = port;
        defaultHost = host;

        JPanel northPanel = new JPanel(new GridLayout(3, 1));

        JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));

        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

        serverAndPort.add(new JLabel("Server Address:  "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel("Port Number:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel(""));

        northPanel.add(serverAndPort);

        //Label and the TextField
        label = new JLabel("Enter your username below", SwingConstants.CENTER);
        northPanel.add(label);
        m_tf = new JTextField("Anonymous");
        m_tf.setBackground(Color.WHITE);
        northPanel.add(m_tf);
        m_frame.add(northPanel, BorderLayout.NORTH);

        // chat room panel
        ta = new JTextArea("Welcome to the Chat room\n", 80, 80);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);
        m_frame.add(centerPanel, BorderLayout.CENTER);
        
        m_buttonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Object o = ae.getSource();
                // if it is the Logout button
                if (o == logout) {
                    client.sendMessage(new ChatMessage(MessageType.LOGOUT, ""));
                    return;
                }
                // if it the who is in button
                if (o == whoIsIn) {
                    client.sendMessage(new ChatMessage(MessageType.WHOS_ONLINE, ""));
                    return;
                }
                if (connected) {
                    client.sendMessage(new ChatMessage(MessageType.MESSAGE, m_tf.getText()));
                    m_tf.setText("");
                    return;
                }
                if (o == login) {
                    String username = m_tf.getText().trim();
                    if (username.length() == 0) {
                        return;
                    }
                    String server = tfServer.getText().trim();
                    if (server.length() == 0) {
                        return;
                    }
                    String portNumber = tfPort.getText().trim();
                    if (portNumber.length() == 0) {
                        return;
                    }
                    int port = 0;
                    try {
                        port = Integer.parseInt(portNumber);
                    } catch (Exception en) {
                        return;
                    }
                    client = new Client(server, port, username, ClientGUI.this);
                    if (!client.start()) {
                        return;
                    }
                    m_tf.setText("");
                    label.setText("Enter your message below");
                    connected = true;

                    // disable login button
                    login.setEnabled(false);
                    // enable the 2 buttons
                    logout.setEnabled(true);
                    whoIsIn.setEnabled(true);
                    // disable the server and port JTextField
                    tfServer.setEditable(false);
                    tfPort.setEditable(false);
                    m_tf.addActionListener(this);                
                }
            }
        };

        login = new JButton("Login");
        login.addActionListener(m_buttonListener);
        logout = new JButton("Logout");
        logout.addActionListener(m_buttonListener);
        logout.setEnabled(false);		// you have to login before being able to logout
        whoIsIn = new JButton("Who is in");
        whoIsIn.addActionListener(m_buttonListener);
        whoIsIn.setEnabled(false);
        

        JPanel southPanel = new JPanel();
        southPanel.add(login);
        southPanel.add(logout);
        southPanel.add(whoIsIn);
        m_frame.add(southPanel, BorderLayout.SOUTH);

        m_frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        m_frame.setSize(600, 600);
        m_frame.setVisible(true);
        m_tf.requestFocus();

    }

    void append(String str) {
        ta.append(str);
        ta.append(System.lineSeparator());
        ta.setCaretPosition(ta.getText().length() - 1);
    }

    void connectionFailed() {
        login.setEnabled(true);
        logout.setEnabled(false);
        whoIsIn.setEnabled(false);
        label.setText("Enter your username below");
        //tf.setText("Anonymous");
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        tfServer.setEditable(false);
        tfPort.setEditable(false);
        m_tf.removeActionListener(m_buttonListener);
        connected = false;
    }

    
    public static void main(String[] args) {
        new ClientGUI("localhost", 60000);
    }
}
