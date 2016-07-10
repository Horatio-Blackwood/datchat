package datchat.server;

import datchat.UserStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The controller implementation for the server.
 * @author adam
 */
public class ServerController implements ServerListener, ServerDisplayListener {
    
    private final Server m_server;
    private final List<ServerOutputHandler> m_outHandlers;
    
    public ServerController(Server server, ServerOutputHandler... sohs) {
        m_server = server;
        m_outHandlers = new ArrayList<>();
        m_outHandlers.addAll(Arrays.asList(sohs));
    }

    @Override
    public void handleServerLogOutput(String logMessage) {
        m_outHandlers.stream().forEach((soh) -> {
            soh.handleEventMsg(logMessage);
        });
    }

    @Override
    public void handleServerMessageOutput(String message) {
        m_outHandlers.stream().forEach((soh) -> {
            soh.handleChatRoomMsg(message);
        });
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
