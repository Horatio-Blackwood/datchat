package datchat.server;

/**
 *
 * @author adam
 */
public interface ServerOutputHandler {
    
    public void handleChatRoomMsg(String msg);
    
    public void handleEventMsg(String msg);
}
