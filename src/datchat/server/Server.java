package datchat.server;

import datchat.ChatMessage;
import datchat.Datchat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;



/**
 * The chat Server main.
 * @author victor
 * @author adam
 */
public class Server {

    /** a unique ID for each connection. */
    private static int m_uniqueId;

    /** An array of client threads. */
    private final ArrayList<ClientThread> m_clientThreads;

    /** A date formatter. */
    private static final SimpleDateFormat MSG_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

    /** The port to listen for clients on. */
    private int m_port;

    /** The boolean that will be turned of to stop the server. */
    private AtomicBoolean m_continue;

    /** A list of ServerListeners. */
    private final List<ServerListener> m_listeners;

    /** The server socket where we listen for client connections. */
    private ServerSocket m_serverSocket;


    /** Creates a new server with the specified port and display. */
    public Server() {
        m_port = Datchat.DEFAULT_PORT;
        m_clientThreads = new ArrayList<>();
        m_listeners = new ArrayList<>();
        m_continue = new AtomicBoolean(false);
    }

    /**
     * Adds a new server listener to this server to be informed of server events.
     * @param sl the listener to add.
     */
    public void addServerListener(ServerListener sl) {
        if (sl != null) {
            m_listeners.add(sl);
        }
    }

    /**
     * Called to start the server.
     * @param port the port to connect on.
     */
    public void start(int port) {
        showServerOutput("Starting on port:  " + port);
        m_port = port;
        m_continue = new AtomicBoolean(true);
        try {
            // the socket used by the server
            m_serverSocket = new ServerSocket(m_port);
            while (m_continue.get()) {
                showServerOutput("Server waiting for Clients on port " + m_port + ".");

                // accept connection
                Socket socket = m_serverSocket.accept();

                // This is weird....
                if (!m_continue.get()) {
                    break;
                }

                // make a thread
                ClientThread t = new ClientThread(socket);
                // save it in the ArrayList
                m_clientThreads.add(t);
                t.start();
            }

            showServerOutput("Closing down all client conections.");
            closeSockets();


        } catch (IOException e) {
            String msg = MSG_DATE_FORMAT.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            showServerOutput(msg);
        }
    }

    /** Stop the server. */
    protected void stop() {
        for (ServerListener sl : m_listeners) {
            sl.handleServerLogOutput("Stop requested.");
        }
        m_continue.set(false);
        closeSockets();
    }

    /** Closes the connection socket and all client sockets. */
    private void closeSockets() {
        try {
            // Close the server socket.
            m_serverSocket.close();

            // Close the client connections.
            for (int i = 0; i < m_clientThreads.size(); ++i) {
                ClientThread ct = m_clientThreads.get(i);
                try {
                    ct.sInput.close();
                    ct.sOutput.close();
                    ct.socket.close();
                } catch (IOException ioe) {
                    showServerOutput("Error closing socket for:  " + ct.username);
                }
            }

        } catch (Exception e) {
            showServerOutput("Exception closing the server and clients: " + e);
        }
    }

    /**
     * Displays the supplied server log/output (either to the GUI or to the console).
     * @param msg the message to show.
     */
    private void showServerOutput(String msg) {
        String message = MSG_DATE_FORMAT.format(System.currentTimeMillis()) + " " + msg;
        for (ServerListener sl : m_listeners) {
            sl.handleServerLogOutput(message);
        }
    }

    /**
     * Displays a chat room message (either to the GUI or the console).
     * @param msg the message to show.
     */
    private void showRoomMessage(String msg) {
        for (ServerListener sl : m_listeners) {
            sl.handleServerMessageOutput(msg);
        }
    }

    /**
     * Broadcasts a message to the users in the chat room.
     * @param message the message to send out.
     */
    private synchronized void broadcast(String message) {
        String time = MSG_DATE_FORMAT.format(System.currentTimeMillis());
        StringBuilder bldr = new StringBuilder(time);
        bldr.append(" - ");
        bldr.append(message);
        String formattedMessage = bldr.toString();

        // Show Chat Room Message
        showRoomMessage(formattedMessage);

        // Send to Clients
        for (int i = m_clientThreads.size(); --i >= 0;) {

            // Send message to client
            ClientThread ct = m_clientThreads.get(i);
            boolean sendFailed = !ct.writeMsg(formattedMessage);

            // If we failed to send a message to a given client, their connection is bad, remove them from the list of clients.
            if (sendFailed) {
                m_clientThreads.remove(i);
                showServerOutput("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    /**
     * Removes the client with the supplied ID from the list of ClientThreads.
     * TODO - make client list a map?
     * @param id the ID of the client to remove.
     */
    synchronized void remove(int id) {
        for (int i = 0; i < m_clientThreads.size(); ++i) {
            ClientThread ct = m_clientThreads.get(i);
            if (ct.id == id) {

                // Disconnect from client.
                try {
                    ct.socket.close();
                } catch (IOException ex) {
                    showServerOutput("IO Exception closing client socket:  " + ct.username + ".");
                }

                m_clientThreads.remove(i);
                showServerOutput("Removed client:  " + ct.username);
                //showRoomMessage(ct.username + " disconnected.");
                broadcast(ct.username + " disconnected.");
                return;
            }
        }
    }

    /**
     * Test Main.
     * @param args ignored.
     */
    public static void main(String[] args) {
        Server s = new Server();
        ServerDisplay sd = new ServerDisplay(60000);
        ServerController controller = new ServerController(s, sd);

        // Add the listeners
        s.addServerListener(controller);
        sd.addServerDisplayListener(controller);

        // Launch GUI.
        sd.launchDisplay();
    }

    /** Helper class that represents each connected chat user. */
    class ClientThread extends Thread {

        /** Socket for listening for messages from the chat user and publishing messages to them. */
        Socket socket;
        /** In stream for incoming messages. */
        ObjectInputStream sInput;
        /** Out stream for outgoing messages. */
        ObjectOutputStream sOutput;
        /** A user id. */
        int id;
        /** A username for the user. */
        String username;
        /** A ChatMessage object used to read the ChatMessage objects from the socket. */
        ChatMessage m_msg;
        /** The time the user connected to the server. */
        String connectionTime;

        /**
         * Constructor.
         * @param socket the socket the client connected on.
         */
        ClientThread(Socket socket) {
            id = ++m_uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                // create output
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String)sInput.readObject();

                // Publish connection information.
                String join = username + " has connected.";
                showServerOutput(join);
                broadcast(join);

            } catch (IOException e) {
                showServerOutput("Exception creating new Input/output Streams: " + e);
                return;
            } catch (ClassNotFoundException e) {
            }
            connectionTime = MSG_DATE_FORMAT.format(System.currentTimeMillis());
        }

        @Override
        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                try {
                    m_msg = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    showServerOutput(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                String message = m_msg.getMessage();

                switch (m_msg.getType()) {

                    case MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case LOGOUT:
                        showServerOutput(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case WHOS_ONLINE:
                        StringBuilder bldr = new StringBuilder();
                        int clients = m_clientThreads.size();
                        if (clients == 1) {
                            bldr.append("You are the only one online.  How sad :(");
                        } else {
                            bldr.append("\nThere are ");
                            bldr.append(String.valueOf(m_clientThreads.size()));
                            bldr.append(" users connected as of ");
                            bldr.append(MSG_DATE_FORMAT.format(System.currentTimeMillis()));
                            bldr.append(System.lineSeparator());
                            // scan all the users connected
                            for (int i = 0; i < m_clientThreads.size(); ++i) {
                                ClientThread ct = m_clientThreads.get(i);
                                bldr.append("   ");
                                bldr.append(i + 1);
                                bldr.append(") ");
                                bldr.append(ct.username);
                                bldr.append(System.lineSeparator());
                                bldr.append("      - from ");
                                bldr.append(socket.getInetAddress().getHostAddress());
                                bldr.append(System.lineSeparator());
                                bldr.append("      - since ");
                                bldr.append(ct.connectionTime);
                                bldr.append(System.lineSeparator());
                            }
                        }
                        writeMsg(bldr.toString());
                        break;
                }
            }
            remove(id);
            close();
        }

        @SuppressWarnings("empty-statement")
        private void close() {
            // try to close the connection
            try {
                if (sOutput != null) {
                    sOutput.close();
                }
            } catch (Exception e) {
            }
            try {
                if (sInput != null) {
                    sInput.close();
                }
            } catch (Exception e) {
            };
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
            }
        }

        private boolean writeMsg(String msg) {
            // if client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                showServerOutput("Error sending message to " + username);
                showServerOutput(e.toString());
            }
            return true;
        }
    }
}
