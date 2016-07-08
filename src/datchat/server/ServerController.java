package datchat.server;

/**
 * The controller implementation for the server.
 * @author adam
 */
public class ServerController implements ServerListener, ServerDisplayListener {
    
    private Server m_server;
    private ServerDisplay m_display;
    
    public ServerController(Server server, ServerDisplay display) {
        m_server = server;
        m_display = display;
    }

    @Override
    public void handleServerLogOutput(String logMessage) {
        m_display.appendEvent(logMessage);
    }

    @Override
    public void handleServerMessageOutput(String message) {
        m_display.appendRoom(message);
    }

    @Override
    public void requestServerStop() {
        m_server.stop();
    }

    @Override
    public void requestServerStart(int port) {
        m_server.start(port);
    }
}
