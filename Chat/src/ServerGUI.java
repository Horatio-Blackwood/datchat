/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author victor
 */
public class ServerGUI extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 1L;
    private JButton stopStart;
    private JTextArea chat, event;
    private JTextField tPortNumber;
    private Server server;

    // server constructor that receive the port to listen to for connection as parameter
    ServerGUI(int port) {
        super("Chat Server");
        server = null;
        JPanel north = new JPanel();
        north.add(new JLabel("Port number: "));
        tPortNumber = new JTextField("  " + port);
        north.add(tPortNumber);
        stopStart = new JButton("Start");
        stopStart.addActionListener(this);
        north.add(stopStart);
        add(north, BorderLayout.NORTH);

        // the event and chat room
        JPanel center = new JPanel(new GridLayout(2, 1));
        chat = new JTextArea(80, 80);
        chat.setEditable(false);
        appendRoom("Chat room.\n");
        center.add(new JScrollPane(chat));
        event = new JTextArea(80, 80);
        event.setEditable(false);
        appendEvent("Events log.\n");
        center.add(new JScrollPane(event));
        add(center);

        addWindowListener(this);
        setSize(400, 600);
        setVisible(true);
    }

    void appendRoom(String str) {
        chat.append(str);
        chat.setCaretPosition(chat.getText().length() - 1);
    }

    void appendEvent(String str) {
        event.append(str);
        event.setCaretPosition(chat.getText().length() - 1);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // if running it have to stop
        if (server != null) {
            server.stop();
            server = null;
            tPortNumber.setEditable(true);
            stopStart.setText("Start");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(tPortNumber.getText().trim());
        } catch (Exception er) {
            appendEvent("Invalid port number");
            return;
        }
        // ceate a new Server
        server = new Server(port, this);
        // start a thread
        new ServerRunning().start();
        stopStart.setText("Stop");
        tPortNumber.setEditable(false);
    }

    public static void main(String[] arg) {
        new ServerGUI(1500);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        if (server != null) {
            try {
                server.stop();			// ask the server to close the conection
            } catch (Exception eClose) {
            }
            server = null;
        }
        // dispose the frame
        dispose();
        System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    class ServerRunning extends Thread {

        @Override
        public void run() {
            server.start();         // should execute until if fails
            // the server failed
            stopStart.setText("Start");
            tPortNumber.setEditable(true);
            appendEvent("Server crashed\n");
            server = null;
        }
    }

}
