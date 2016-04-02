package datchat.client;

import datchat.ChatMessage;

/**
 * An interface describing the contract of a Client Display Listener.  CDLs respond to display events and request
 * associated Client objects to take action based on those events.
 * 
 * @author adam
 */
public interface ClientDisplayListener {
   
    /**
     * Signals the Client Controller to connect to a Client object.
     * @param host The hostname or ip address of the server to connect to.
     * @param port the port number to connect on.
     * @param username the username to connect to the server with.
     * @return true if a connection is made, false otherwise.
     */
    public boolean connect(String host, int port, String username);
    
    /**
     * Signals the a connected client to send the supplied chat message.
     * @param msg the message to send.
     */
    public void sendMessage(ChatMessage msg);
}
