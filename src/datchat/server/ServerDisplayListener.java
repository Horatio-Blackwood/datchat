package datchat.server;

/**
 *
 * @author adam
 */
public interface ServerDisplayListener {
    
    public void requestServerStop();
    
    public void requestServerStart(int port);
    
}
