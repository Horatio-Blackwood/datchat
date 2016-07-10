package datchat.server;

import datchat.Datchat;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


/**
 * A Display for the server.
 * 
 * @author victor
 * @author adam
 */
public class ServerDisplay {

    private static final long serialVersionUID = 1L;
    
    private JFrame m_frame;
    private JButton m_stopStart;
    private JTextArea m_chat;
    private JTextArea m_event;
    private JTextField m_tPortNumber;
    private final ArrayList<ServerDisplayListener> m_listeners;
    private final AtomicBoolean m_started = new AtomicBoolean(false);

    /**
     * Constructs a new Server Display with the supplied default port.
     * @param defaultPort the default port for the display to show in the Port box.
     */
    public ServerDisplay(int defaultPort) {
        m_listeners = new ArrayList<>();
        initializeDisplay(defaultPort);
    }
    
    /**
     * Adds a new ServerDisplayListener to this display.
     * @param listener the listener to add.
     */
    public void addServerDisplayListener(ServerDisplayListener listener) {
        if (listener != null) {
            m_listeners.add(listener);
        }
    }
    
    private void initializeDisplay(int port) {
        m_frame = new JFrame("DatChat Server - " + Datchat.VERSION);
        
        // Initialize Server Configuration Panel and components.
        JPanel serverConfigPanel = new JPanel(new GridBagLayout());
        m_tPortNumber = new JTextField(String.valueOf(port), 4);
        m_stopStart = new JButton("Start");
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (m_started.get()) {
                    // If its started, we're stopping it.  Set started to false, and set the 
                    // button to be a start button for the next push.
                    m_started.set(false);
                    m_stopStart.setText("Start");
                } else {
                    // If we're stopped, we're startin git.  Set started to true and set the 
                    // button to be a 'Stop' button for the next push.
                    m_started.set(true);
                    m_stopStart.setText("Stop");
                }
                
                for (ServerDisplayListener sdl : m_listeners) {
                    // Handle Stop Request.
                    if (sdl != null) {
                        if (m_started.get()) {
                            int port = Datchat.DEFAULT_PORT;
                            try {
                                port = Integer.parseInt(m_tPortNumber.getText().trim());
                            } catch (Exception er) {
                                appendEvent("Invalid port number.  Using default of " + Datchat.DEFAULT_PORT);
                            }
                            ServerThread st = new ServerThread(sdl, port);
                            st.start();
                            m_tPortNumber.setEditable(false);
                            return;                        
                        } else {
                            appendEvent("Requesting server stop.");
                            sdl.requestServerStop();
                            m_tPortNumber.setEditable(true);
                            return;
                        }
                    }
                }
            }
        };
        m_stopStart.addActionListener(al);        
        
        // Layout Configuration Panel Components
        // ROW 1
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        //gbc.weightx = 1.0f;
        serverConfigPanel.add(new JLabel("Server Port:"), gbc);
        
        gbc.gridx = 1;
        serverConfigPanel.add(m_tPortNumber, gbc);

        gbc.gridx = 2;
        serverConfigPanel.add(m_stopStart, gbc);
        
        gbc.gridx = 3;
        gbc.weightx = 1.0f;
        serverConfigPanel.add(new JLabel(), gbc);
        
        
        // Layout Frame
        m_frame.add(serverConfigPanel, BorderLayout.NORTH);

        // the event and chat room
        JPanel center = new JPanel(new GridLayout(2, 1));
        // Chat Message Area
        m_chat = new JTextArea(20, 20);
        m_chat.setEditable(false);
        m_chat.setFont(new Font("Monospaced", Font.PLAIN, 13));
        appendRoom("Chat room.\n");
        JScrollPane chatScrollPane = new JScrollPane(m_chat);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        center.add(chatScrollPane);
        
        // Event Message Area
        m_event = new JTextArea(20, 20);
        m_event.setEditable(false);
        m_event.setFont(new Font("Monospaced", Font.PLAIN, 13));
        appendEvent("Events log.\n");
        JScrollPane eventScrollPane = new JScrollPane(m_event);
        eventScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        eventScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        center.add(eventScrollPane);
        m_frame.add(center);

        WindowAdapter windowAdapter = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                for (ServerDisplayListener sdl : m_listeners) {
                    sdl.requestServerStop();
                }
                // dispose the frame
                m_frame.dispose();
                System.exit(0);
            }            
        };
        m_frame.addWindowListener(windowAdapter);
    }
    
    public void launchDisplay() {
        m_frame.setPreferredSize(new Dimension(480, 600));
        m_frame.pack();
        m_frame.setVisible(true);
    }

    /**
     * Appends a message to the chat room display.
     * @param str the message to append.
     */
    public void appendRoom(String str) {
        SwingUtilities.invokeLater(() -> {
            m_chat.append(str);
            m_chat.append(System.lineSeparator());
            m_chat.setCaretPosition(m_chat.getText().length() - 1);
        });
    }

    /**
     * Appends a message to the server run log display.
     * @param str the message to append.
     */
    void appendEvent(String str) {
        SwingUtilities.invokeLater(() -> {
            m_event.append(str);
            m_event.append(System.lineSeparator());
            m_event.setCaretPosition(m_event.getText().length() - 1);
        });
    }

    
    class ServerThread extends Thread {
        
        private ServerDisplayListener m_sdl;
        private int m_port;
        private ServerThread(ServerDisplayListener sdl, int port) {
            m_sdl = sdl;
            m_port = port;
        }
        
        @Override
        public void run() {
            m_sdl.requestServerStart(m_port);
            // the server failed
            m_stopStart.setText("Start");
            m_tPortNumber.setEditable(true);
            appendEvent("Server crashed\n");
        }
    }
}
