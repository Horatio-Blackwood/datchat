package datchat.client;

import datchat.ChatMessage;

/**
 *
 * @author adam
 */
public interface ClientDisplayListener {
    
    public boolean connect(String host, int port, String username);
    
    public void sendMessage(ChatMessage msg);
}
