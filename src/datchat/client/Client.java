package datchat.client;

import datchat.ChatMessage;
import static datchat.Datchat.DEFAULT_PORT;
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

    private ClientListener m_listener;

    // the server, the port and the username
    private final String m_serverHost;
    private final String m_username;
    private final int m_port;

    /**
     * Constructor.
     * @param server
     * @param port
     * @param username
     * @param cg 
     */
    Client(String server, int port, String username) {
        m_serverHost = server;
        m_port = port;
        m_username = username;
    }
    
    public void setClientListener(ClientListener listener) {
        m_listener = listener;
    }

    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(m_serverHost, m_port);
        } catch (Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        /* Creating both Data Stream */
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server 
        new ListenFromServer().start();
        // Send our username to the server 
        try {
            sOutput.writeObject(m_username);
        } catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        return true;
    }

    //send a message to the console or the GUI
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
            display("Exception writing to server: " + e);
        }
    }

    private void disconnect() {
        try {
            if (sInput != null) {
                sInput.close();
            }
        } catch (Exception ex) {
        }
        try {
            if (sOutput != null) {
                sOutput.close();
            }
        } catch (Exception ex) {
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

    public static void main(String[] args) {
        ClientDisplay cd = new ClientDisplay("localhost", DEFAULT_PORT);
        ClientController controller = new ClientController(cd);
        cd.addListener(controller);
        controller.launchClient();
    }

    // class that waits for the message from the server 
    class ListenFromServer extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String msg = (String) sInput.readObject();
                    if (m_listener == null) {
                        System.out.println(msg);
                        System.out.print("> ");
                    } else {
                        m_listener.showMessage(msg);
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
