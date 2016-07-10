package datchat.client;

import datchat.ChatMessage;
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
    private ObjectInputStream sInput;
    
    /** To write on the socket. */
    private ObjectOutputStream sOutput;
    
    /** The socket connection to the server. */
    private Socket socket;

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
            socket = new Socket(m_serverHost, m_port);
        } catch (Exception ec) {
            display(" - Error connectiong to server:" + ec);
            return false;
        }

        String msg = " - Server '" + socket.getInetAddress() + ":" + socket.getPort() + "' accepted connection.";
        display(msg);

        /* Creating both Data Stream */
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display(" - Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server 
        new ListenFromServer().start();
        // Send our username to the server 
        try {
            sOutput.writeObject(m_username);
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
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display(" - Exception writing to server: " + e);
        }
    }

    /** Disconnects from the server. */
    private void disconnect() {
        try {
            if (sInput != null) {
                sInput.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (sOutput != null) {
                sOutput.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
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
                    Object obj = sInput.readObject();
                    if (obj instanceof String) {
                        String msg = (String)obj;
                        if (m_listener == null) {
                            System.out.println(msg);
                            System.out.print("> ");
                        } else {
                            m_listener.showMessage(msg);
                        }                        
                    } else if (obj instanceof UserStatus) {
                        UserStatus userStat = (UserStatus)obj;
                        m_listener.updateStatus(userStat);
                    } else {
                        System.out.println("Received unknown message object:  " + obj);
                    }
                    
                } catch (IOException ex) {
                    display("Connection with server closed.");
                    if (m_listener != null) {
                        m_listener.connectionFailed();
                    }
                    break;
                } catch (ClassNotFoundException ex) {
                }
            }
        }
    }
}
