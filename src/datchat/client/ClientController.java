package datchat.client;

import datchat.ChatMessage;

/**
 *
 * @author adam
 */
public class ClientController implements ClientDisplayListener, ClientListener {
    
    private Client m_client;
    private final ClientDisplay m_display;

    public ClientController(ClientDisplay display) {
        m_display = display;
    }
    
    public void launchClient() {
        m_display.launch();
    }
    
    @Override
    public boolean connect(String host, int port, String username) {
        System.out.println("Connecting.");
        m_client = new Client(host, port, username);
        m_client.setClientListener(this);
        return m_client.start();
    }

    @Override
    public void sendMessage(ChatMessage msg) {
        System.out.println("sending msg:  " + msg.getMessage());
        m_client.sendMessage(msg);
    }

    @Override
    public void showMessage(String msg) {
        System.out.println("showing message:  " + msg);
        m_display.showMessage(msg);
    }

    @Override
    public void connectionFailed() {
        System.out.println("connection failed.");
        //m_client.stop(); ???
        m_client = null;
        m_display.connectionFailed();
    }
}
