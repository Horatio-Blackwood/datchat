package datchat.server;

/**
 * 
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
        System.out.println("server log output");
        m_display.appendEvent(logMessage);
    }

    @Override
    public void handleServerMessageOutput(String message) {
        System.out.println("server msg output");
        m_display.appendRoom(message);
    }

    @Override
    public void requestServerStop() {
        System.out.println("r stop");
        m_server.stop();
    }

    @Override
    public void requestServerStart(int port) {
        System.out.println("r start");
        m_server.start(port);
    }
}
