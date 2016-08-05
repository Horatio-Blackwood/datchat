package datchat.server;

import datchat.ChatMessage;
import datchat.Datchat;
import datchat.MessageType;
import datchat.OnlineStatus;
import datchat.UserStatus;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * The chat Server shell.
 * @author adam
 */
public class Server {

    /** a unique ID for each connection. */
    private static int m_uniqueId = 0;

    /** An array of client threads. */
    private final ArrayList<ClientThread> m_clientThreads;

    /** The port to listen for clients on. */
    private int m_port;

    /** The boolean that will be turned of to stop the server. */
    private AtomicBoolean m_continue;

    /** A list of ServerListeners. */
    private final List<ServerListener> m_listeners;

    /** The server socket where we listen for client connections. */
    private ServerSocket m_serverSocket;
    
    /** Someday to be customized by command line args / configuration... */
    private String m_serverName = "SERVER";


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
        showServerLogOutput("Starting on port:  " + port);
        m_port = port;
        m_continue = new AtomicBoolean(true);
        try {
            // the socket used by the server
            m_serverSocket = new ServerSocket(m_port);
            while (m_continue.get()) {
                try {
                    // wait for and accept connection
                    showServerLogOutput("Server waiting for Clients on port " + m_port + ".");
                    Socket socket = m_serverSocket.accept();

                    // This is weird....
                    if (!m_continue.get()) {
                        break;
                    }

                    // make a thread
                    ClientThread t = new ClientThread(socket);
                    t.start();                    
                } catch (Exception e) {
                    // Don't let an error kill the connection thread...
                    showServerLogOutput("Error occured listening for clients:  " + e.getMessage());
                }                
            }

            showServerLogOutput("Closing down all client conections.");
            closeSockets();


        } catch (IOException e) {
            String msg = Datchat.CHAT_TIME_FORMATTER.format(System.currentTimeMillis()) + " Exception on new ServerSocket: " + e + "\n";
            showServerLogOutput(msg);
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
                    showServerLogOutput("Error closing socket for:  " + ct.username);
                }
            }

        } catch (Exception e) {
            showServerLogOutput("Exception closing the server and clients: " + e);
        }
    }

    /**
     * Displays the supplied server log/output (either to the GUI or to the console).
     * @param msg the message to show.
     */
    private void showServerLogOutput(String msg) {
        m_listeners.stream().forEach((sl) -> {
            sl.handleServerLogOutput(msg);
        });
    }

    /**
     * Displays a chat room message (either to the GUI or the console).
     * @param msg the message to show.
     */
    private void showRoomMessage(String msg) {
        m_listeners.stream().forEach((sl) -> {
            sl.handleServerMessageOutput(msg);
        });
    }

    
    private synchronized void broadcastChatMessage(ChatMessage msg) {
        // Do Server Side Message Processing
        MessageType type = msg.getType();
        switch (type) {
            case USER_STATUS:
                UserStatus status = (UserStatus)msg.getMessage();
                showServerLogOutput(status.toString());
                break;
            case LOGOUT:
                // Do nothing?
                break;
            case CHAT_MESSAGE:
                String message = Datchat.CHAT_TIME_FORMATTER.format(System.currentTimeMillis()) + "  " + msg.getMessage();
                showRoomMessage(message);
                
                // Update message for client processing with newly formatted chat message contents (date/time group etc.)
                msg = new ChatMessage(MessageType.CHAT_MESSAGE, message);
                break;
        }
        
        // Once Server Side Processing is complet, send the message to the Clients.
        // --- For Each Client...
        for (int i = m_clientThreads.size(); --i >= 0;) {

            // Send message to client
            ClientThread ct = m_clientThreads.get(i);
            boolean sendFailed = !ct.writeMsg(msg);

            // If we failed to send a message to a given client, their connection is bad, remove them from the list of clients.
            if (sendFailed) {
                remove(ct.id);
                showServerLogOutput("Disconnected Client " + ct.username + " removed from list.");
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
                ct.close();

                m_clientThreads.remove(i);
                showServerLogOutput("Removed client:  " + ct.username);
                
                // Broadcast Disconnect chat room message
                String broadcastMsg = prepareMsgForBroadcast(m_serverName, ct.username + " disconnected.");
                broadcastChatMessage(new ChatMessage(MessageType.CHAT_MESSAGE, broadcastMsg));
                
                // Broadcast UserStatus Message object:
                UserStatus userStat = ct.getUserStatus(OnlineStatus.OFFLINE);
                ChatMessage status = new ChatMessage(MessageType.USER_STATUS, userStat);
                broadcastChatMessage(status);
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
    
    private String prepareMsgForBroadcast(String username, String message) {
        // Create the message to broadcast, doing some pretty printing 
        StringBuilder broadcastMsg = new StringBuilder(username);
        broadcastMsg.append(":");
        if (username.length() < Datchat.MAX_USERNAME_CHARS) {
            int spaces = Datchat.MAX_USERNAME_CHARS - username.length();
            for (int i = 0; i < spaces; i++) {
                broadcastMsg.append(" ");
            }
        }
        broadcastMsg.append("  ");
        broadcastMsg.append(message);    
        
        return broadcastMsg.toString();
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
            connectionTime = System.currentTimeMillis();
        }
        
        UserStatus getUserStatus(OnlineStatus oStat) {
            return new UserStatus(username, socket.getInetAddress().getHostName(), connectionTime, oStat);
        }

        @Override
        public void run() {
            boolean keepGoing = true;
            try {
                // Create out and input streams.
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                
                // Wait for the client to send you a username.  If this does not occur, we have an invalid client.
                username = (String)sInput.readObject();
                
                // Alert ot her clients that this client has successfully connected.
                String join = username + " has connected.";
                showServerLogOutput(join);
                String broadcastMsg = prepareMsgForBroadcast(m_serverName, join);
                broadcastChatMessage(new ChatMessage(MessageType.CHAT_MESSAGE, broadcastMsg));

            } catch (IOException | ClassNotFoundException e) {
                showServerLogOutput("Exception creating new Input/output Streams: " + e);
                return;
            }
            
            // Add this client to the list of connected clients - DO THIS ONLY AFTER SUCCESSFULLY GETTING A USERNAME.
            m_clientThreads.add(this);            
            
            // Publish all of the online user statuses known to this newly connected user.
            m_clientThreads.stream().forEach((ct) -> {
                writeMsg(new ChatMessage(MessageType.USER_STATUS, ct.getUserStatus(OnlineStatus.ONLINE)));
            });
            
            // Publish This Client's Own User Status to the othert users.
            UserStatus userStat = getUserStatus(OnlineStatus.ONLINE);
            ChatMessage status = new ChatMessage(MessageType.USER_STATUS, userStat);
            broadcastChatMessage(status);

            while (keepGoing) {
                try {
                    try {
                        m_msg = (ChatMessage) sInput.readObject();
                    } catch (IOException e) {
                        showServerLogOutput(username + " Exception reading Streams: " + e);
                        break;
                    }
                    
                    // Determine Message Type and Handle it.
                    switch (m_msg.getType()) {
                        case CHAT_MESSAGE:
                            // Get the payload (a String) and smash-cast it to a String for further use.
                            String message = (String)m_msg.getMessage();
                            
                            // Prepare/Format the msg and send it.
                            String broadcastMsg = prepareMsgForBroadcast(username, message);
                            broadcastChatMessage(new ChatMessage(MessageType.CHAT_MESSAGE, broadcastMsg));
                            break;
                        case LOGOUT:
                            showServerLogOutput(username + " disconnected with a LOGOUT message.");
                            keepGoing = false;
                            break;
                    }
                } catch (Exception e) {
                    // General catch-all Not a long-term resident in this class, but in early development...
                    showServerLogOutput("Error occured processing thread for client:  " + this.username);
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
            } catch (IOException e) {
                showServerLogOutput("IO Exception closing client output stream:  " + this.username + ".");
            }
            try {
                if (sInput != null) {
                    sInput.close();
                }
            } catch (IOException e) {
                showServerLogOutput("IO Exception closing client input stream:  " + this.username + ".");
            }
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                showServerLogOutput("IO Exception closing client socket:  " + this.username + ".");
            }
        }

        /**
         * Writes the supplied ChatMessage object to the output stream.
         * @param msg the ChatMessage object to write.
         * @return true if the writing was successful, false otherwise.
         */
        private boolean writeMsg(ChatMessage msg) {
            // if client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
                return true;
            } catch (IOException e) {
                showServerLogOutput("Error sending message to " + username);
                showServerLogOutput(e.toString());
                return false;
            }
        }
    }
}
