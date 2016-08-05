package datchat.client;

import datchat.ChatMessage;
import datchat.MessageType;
import datchat.UserStatus;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The Chat Client Engine.
 * 
 * @author victor
 * @author adam
 */

public class Client {

    /** To read from the socket. */
    private ObjectInputStream m_sInput;
    
    /** To write on the socket. */
    private ObjectOutputStream m_sOutput;
    
    /** The socket connection to the server. */
    private Socket m_socket;

    /** A client listener to alert of events from the client app (model). */
    private ClientListener m_listener;

    /** Username string. */
    private final String m_username;
    
    /** the server, the port and the username. */
    private final String m_serverHost;
    
    /** The port to connect to the server on. */
    private final int m_port;

    /**
     * Constructor.
     * @param server the hostname / ip address of the server.
     * @param port  the port to connect to the server on.
     * @param username the username of the user of this client.
     */
    Client(String server, int port, String username) {
        m_serverHost = server;
        m_port = port;
        m_username = username;
    }
    
    /**
     * Sets the client listener to be alerted of events from this Client.
     * @param listener the listener to set.  if null, the listener is cleared.
     */
    public void setClientListener(ClientListener listener) {
        m_listener = listener;
    }

    /**
     * Called to start the client.
     * @return 
     */
    public boolean start() {
        // try to connect to the server
        try {
            m_socket = new Socket(m_serverHost, m_port);
        } catch (Exception ec) {
            display(" - Error connectiong to server:" + ec);
            return false;
        }

        String msg = " - Server '" + m_socket.getInetAddress() + ":" + m_socket.getPort() + "' accepted connection.";
        display(msg);

        /* Creating both Data Stream */
        try {
            m_sInput = new ObjectInputStream(m_socket.getInputStream());
            m_sOutput = new ObjectOutputStream(m_socket.getOutputStream());
        } catch (IOException eIO) {
            display(" - Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server 
        new ListenFromServer().start();
        // Send our username to the server 
        try {
            m_sOutput.writeObject(m_username);
        } catch (IOException eIO) {
            display(" - Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        return true;
    }

    /**
     * Sends a message to the console or the GUI.  This should be used for received msgs only, not for messages 
     * being published out.
     * @param msg the message to send to the display.
     */
    private void display(String msg) {
        if (m_listener != null) {
            m_listener.showMessage(msg);
        } else {
            System.out.println(msg);
        }
    }

    /**
     * Sends the supplied message to the server.
     * @param msg the message to send.
     */
    void sendMessage(ChatMessage msg) {
        try {
            m_sOutput.writeObject(msg);
        } catch (IOException e) {
            display(" - Exception writing to server: " + e);
        }
    }

    /** Disconnects from the server. */
    private void disconnect() {
        try {
            if (m_sInput != null) {
                m_sInput.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (m_sOutput != null) {
                m_sOutput.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (m_socket != null) {
                m_socket.close();
            }
        } catch (Exception ex) {
        }
        if (m_listener != null) {
            m_listener.connectionFailed();
        }
    }

    /** Helper class that runs in a separate thread to listen for messages from the server ti be displayed. */
    class ListenFromServer extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    ChatMessage cm = (ChatMessage)m_sInput.readObject();
                    MessageType type = cm.getType();
                    
                    // The client will switch here on message type, using that information to figure out what
                    // sort of payload the received message has.  Once that has been determined, the client will
                    // cast the payload and perform whatever work is necessary.  Client Display Listener and Client 
                    // Listener should not operate on ChatMessages - the message payloads should be extracted here 
                    // and appropriate type sbe used for the APIs of these objects.
                    switch(type) {
                        case CHAT_MESSAGE:
                            String msg = (String)cm.getMessage();
                            if (m_listener == null) {
                                System.out.println("> " + msg);
                            } else {
                                m_listener.showMessage(msg);
                            }
                            break;
                        case LOGOUT:
                            break;
                        case USER_STATUS:
                            UserStatus status = (UserStatus)cm.getMessage();
                            if (m_listener == null) {
                                System.out.println("> " + status.toString());
                            } else {
                                m_listener.updateStatus(status);
                            }
                            break;
                        default:
                            String errMsg = "Received unknown message type:  " + type + ", with message payloiad:  " + cm.getMessage();
                            if (m_listener == null) {
                                System.out.println(errMsg);
                            } else {
                                m_listener.showMessage(errMsg);
                            }
                    }
                } catch (IOException ex) {
                    display("Connection with server closed.");
                    if (m_listener != null) {
                        m_listener.connectionFailed();
                    }
                    break;
                } catch (ClassNotFoundException ex) {
                    // This should never happen.
                    ex.printStackTrace();
                }
            }
        }
    }
}
