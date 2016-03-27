package datchat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 * @author victor
 */

public class Client {

    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;

    private ClientGUI cg;

    // the server, the port and the username
    private String server, username;
    private int port;

    /*
     *  Constructor called by console mode
     *  server: the server address
     *  port: the port number
     *  username: the username
     */
    Client(String server, int port, String username) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username, null);
    }

    /*
     * Constructor call when used from a GUI
     * in console mode the ClienGUI parameter is null
     */
    Client(String server, int port, String username, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        // save if we are in GUI mode or not
        this.cg = cg;
    }

    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        } catch (Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        /* Creating both Data Stream */
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server 
        new ListenFromServer().start();
        // Send our username to the server 
        try {
            sOutput.writeObject(username);
        } catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        return true;
    }

    //send a message to the console or the GUI
    private void display(String msg) {
        if (cg == null) {
            System.out.println(msg);
        } else {
            cg.append(msg);
        }
    }

    // send message to the server
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    private void disconnect() {
        try {
            if (sInput != null) {
                sInput.close();
            }
        } catch (Exception ex) {
        }
        try {
            if (sOutput != null) {
                sOutput.close();
            }
        } catch (Exception ex) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ex) {
        }
        if (cg != null) {
            cg.connectionFailed();
        }
    }

    public static void main(String[] args) {
        // default values
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";

        switch (args.length) {
            case 3:
                serverAddress = args[2];
            case 2:
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
            case 1:
                userName = args[0];
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
        if (!client.start()) {
            return;
        }

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while (true) {
            System.out.print("> ");
            String msg = scan.nextLine();
            if (msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(MessageType.LOGOUT, ""));
                break;
            } else if (msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(MessageType.WHOS_ONLINE, ""));
            } else {
                client.sendMessage(new ChatMessage(MessageType.MESSAGE, msg));
            }
        }
        client.disconnect();
    }

    // class that waits for the message from the server 
    class ListenFromServer extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    String msg = (String) sInput.readObject();
                    if (cg == null) {
                        System.out.println(msg);
                        System.out.print("> ");
                    } else {
                        cg.append(msg);
                    }
                } catch (IOException ex) {
                    display("Connection with server closed.");
                    if (cg != null) {
                        cg.connectionFailed();
                    }
                    break;
                } catch (ClassNotFoundException ex) {
                }
            }
        }
    }
}
