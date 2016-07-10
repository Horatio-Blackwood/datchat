package datchat.server;

import datchat.ChatMessage;
import datchat.Datchat;
import datchat.OnlineStatus;
import datchat.UserStatus;
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
 * The chat Server shell.
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
                try {
                    // wait for and accept connection
                    showServerOutput("Server waiting for Clients on port " + m_port + ".");
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
                } catch (Exception e) {
                    // Don't let an error kill the connection thread...
                    showServerOutput("Error occured listening for clients:  " + e.getMessage());
                }                
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
        String message = MSG_DATE_FORMAT.format(System.currentTimeMillis()) + "  " + msg;
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
    private synchronized void broadcast(String msg) {
        String message = MSG_DATE_FORMAT.format(System.currentTimeMillis()) + "  " + msg;
        // Show Chat Room Message
        showRoomMessage(message);

        // Send to Clients
        for (int i = m_clientThreads.size(); --i >= 0;) {

            // Send message to client
            ClientThread ct = m_clientThreads.get(i);
            boolean sendFailed = !ct.writeMsg(message);

            // If we failed to send a message to a given client, their connection is bad, remove them from the list of clients.
            if (sendFailed) {
                m_clientThreads.remove(i);
                showServerOutput("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }
    
    /**
     * Broadcasts a message to the users in the chat room.
     * @param message the message to send out.
     */
    private synchronized void broadcastObjMessage(Object msg) {
        String message = MSG_DATE_FORMAT.format(System.currentTimeMillis()) + "  " + msg.toString();

        // Show Chat Room Message
        showServerOutput(message);

        // Send to Clients
        for (int i = m_clientThreads.size(); --i >= 0;) {

            // Send message to client
            ClientThread ct = m_clientThreads.get(i);
            boolean sendFailed = !ct.writeMsg(msg);

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
                
                // Broadcast Disconnect chat room message
                broadcast(ct.username + " disconnected.");
                
                // Broadcast UserStatus Message object:
                UserStatus userStat = new UserStatus(ct.username, ct.socket.getInetAddress().getHostName(), ct.connectionTime, OnlineStatus.OFFLINE);
                broadcastObjMessage(userStat);
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
        long connectionTime;

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
            connectionTime = System.currentTimeMillis();
        }
        
        UserStatus getUserStatus(OnlineStatus oStat) {
            return new UserStatus(username, socket.getInetAddress().getHostName(), connectionTime, oStat);
        }

        @Override
        public void run() {
            boolean keepGoing = true;
            
            // Publish all of the online user statuses known to this newly connected user.
            m_clientThreads.stream().forEach((ct) -> {
                writeMsg(ct.getUserStatus(OnlineStatus.ONLINE));
            });
            
            // Publish This Client's Own User Status to the othert users.
            UserStatus userStat = getUserStatus(OnlineStatus.ONLINE);
            broadcastObjMessage(userStat);
                        
            while (keepGoing) {
                try {
                    try {
                        m_msg = (ChatMessage) sInput.readObject();
                    } catch (IOException e) {
                        showServerOutput(username + " Exception reading Streams: " + e);
                        break;
                    }
                    
                    // Determine Message Type and Handle it.
                    String message = m_msg.getMessage();
                    switch (m_msg.getType()) {
                        case MESSAGE:
                            String longest = "";
                            for (ClientThread ct : m_clientThreads) {
                                if (ct.username.length() > longest.length()) {
                                    longest = ct.username;
                                }
                            }
                            
                            // Create the message to broadcast, doing some pretty printing 
                            // based on name length to align thee chat messages.
                            StringBuilder broadcastMsg = new StringBuilder(username);
                            broadcastMsg.append(":");
                            if (username.length() < longest.length()) {
                                int spaces = longest.length() - username.length();
                                for (int i = 0; i < spaces; i++) {
                                    broadcastMsg.append(" ");
                                }
                            }
                            broadcastMsg.append("  ");
                            broadcastMsg.append(message);
                            
                            // Send the formatted message.
                            broadcast(broadcastMsg.toString());
                            break;
                        case LOGOUT:
                            showServerOutput(username + " disconnected with a LOGOUT message.");
                            keepGoing = false;
                            break;
                    }
                } catch (Exception e) {
                    // General catch-all Not a long-term resident in this class, but in early development...
                    showServerOutput("Error occured processing thread for client:  " + this.username);
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

        /**
         * Writes the supplied object to object output stream.
         * @param msg the object to write.
         * @return true if the writing was successful, false otherwise.
         */
        private boolean writeMsg(Object msg) {
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
