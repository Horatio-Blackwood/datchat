package datchat.server;

/**
 * This class listens for events from the server and informs interested listeners.
 * @author adam
 */
public interface ServerListener {
    
    public void handleServerLogOutput(String logMessage);
    
    public void handleServerMessageOutput(String message);
    
}
