package datchat;

import datchat.client.ClientController;
import datchat.client.ClientDisplay;
import datchat.server.Server;
import datchat.server.ServerController;
import datchat.server.ServerDisplay;
import datchat.server.ServerLog;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author adam
 */
public class Datchat {

    public static final String VERSION = "v0.5";
    public static final String DATE_CREATED = "4 August 2016";
    public static final int DEFAULT_PORT = 55200;
    
    public static final SimpleDateFormat CHAT_DATE_FORMATTER = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
    public static final SimpleDateFormat CHAT_TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat CHAT_FILE_FORMATTER = new SimpleDateFormat("yy-MM-dd_HH.mm.ss");
    
    public static final int MAX_USERNAME_CHARS = 12;
    
    private static final String SERVER_MODE = "-s";
    private static final String SERVER_LOG_MODE = "-sl";
    private static final String CLIENT_MODE = "-c";
    
    /** Prints out the  usage of this application. */
    private static void printUsage() {
        System.out.println("How to launch Client:");
        System.out.println("   - Requires three args for client:  mode, hostname, IP, and default username");
        System.out.println("   - Example:");
        System.out.println("        java -jar -c chatServer 54200 atombomb");
        
        System.out.println("How to launch Server:");
        System.out.println("   - Requires two args for Server:  mode and IP");
        System.out.println("   - Example:");
        System.out.println("        java -jar -s 54200");
        
        System.out.println("How to launch Server (with logging):");
        System.out.println("   - Requires two args for Server:  mode and IP");
        System.out.println("   - Example:");
        System.out.println("        java -jar -sl 54200");
        
        exit();
    }

    /** Tell the user we're done and quit. */
    private static void exit() {
        System.out.println("System exiting.");
        System.exit(1);        
    }
    
    
    private static int getPort(String portStr) {
        int port = 0;
        try {
            port = Integer.valueOf(portStr);
        } catch (NumberFormatException nfe) {
            System.out.println("Port parameter must be an integral numeric value.  Was:  " + portStr);
            System.exit(1);
        }
        if (port < 1 || port > 65535) {
            System.out.println("Port parameter must be between 1 and 65535.  Was:  " + port);
            System.exit(1);
        }
        
        return port;
    }
    /**
     * Runs DatChat in either server or client mode.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Verifies the right number of args.  Possible fourth arg for setting up an 'admin' password/key?
        if (args.length != 2 && args.length != 4) {
            System.out.println("Did not provide valid argument length must be 2 for server or 4 for client.  Was:  " + args.length);
            printUsage();
            exit();
        }
        
        // Set Host OS Look and Feel.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            System.out.println("Error setting look and feel.");
            ex.printStackTrace();
        }        
        
        
        // validate/store args
        // MODE (server or client) - Arg index 0.
        String mode = args[0];
        int port = 0;
        switch (mode) {
            case SERVER_MODE:
            case SERVER_LOG_MODE:
                if (args.length != 2) {
                    System.out.println("Did not provide valid argument length.");
                    printUsage();
                    exit();
                }
                // Get Port
                port = getPort(args[1]);
                
                // Init Server (model), display (view) and controller.
                Server server = new Server();
                ServerDisplay serverDisplay = new ServerDisplay(port);
                ServerController serverCtrl;
                if (mode.contains("l")) {
                    serverCtrl = new ServerController(server, serverDisplay, new ServerLog());
                } else {
                    serverCtrl = new ServerController(server, serverDisplay);
                }
                
                // Add the listeners
                server.addServerListener(serverCtrl);
                serverDisplay.addServerDisplayListener(serverCtrl);
                

                // Launch GUI.
                serverDisplay.launchDisplay();
                
                break;
                
            case CLIENT_MODE:
                if (args.length != 4) {
                    System.out.println("Did not provide valid argument length.");
                    printUsage();
                    exit();
                }
                // Get host and port from cmd line parameters
                String hostname = args[1];
                port = getPort(args[2]);
                String user = args[3];
                
                // Create and alunch the client display parameters.
                ClientDisplay cd = new ClientDisplay(hostname, port, user);
                ClientController controller = new ClientController(cd);
                controller.launchClient();
                
                break;
            default:
                System.out.println("Invalid mode selected:  " + mode + ".");
                printUsage();
                exit();
        }    
    }
}