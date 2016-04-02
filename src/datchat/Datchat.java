package datchat;

import datchat.client.ClientController;
import datchat.client.ClientDisplay;
import datchat.server.Server;
import datchat.server.ServerController;
import datchat.server.ServerDisplay;
import java.text.SimpleDateFormat;

/**
 *
 * @author adam
 */
public class Datchat {

    public static final String VERSION = "v0.1";
    public static final String DATE_CREATED = "27 March 2015";
    public static final int DEFAULT_PORT = 55200;
    
    public static final SimpleDateFormat CHAT_DATE_FORMATTER = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
    private static final String SERVER_MODE = "-s";
    private static final String CLIENT_MODE = "-c";
    
    /** Prints out the  usage of this application. */
    private static void printUsage() {
        System.out.println("How to launch Client:");
        System.out.println("   - Requires three args for client:  mode, hostname, and IP");
        System.out.println("   - Example:");
        System.out.println("        java -jar -c chatServer 54200");
        
        System.out.println("How to launch Server:");
        System.out.println("   - Requires two args for Server:  mode and IP");
        System.out.println("   - Example:");
        System.out.println("        java -jar -s 54200");
        
        System.out.println("Exiting.");
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
        if (args.length < 0 || args.length > 3) {
            System.out.println("Did not provide valid argument length.");
            printUsage();
            exit();
        }
        
        
        // validate/store args
        // MODE (server or client) - Arg index 0.
        String mode = args[0];
        int port = 0;
        switch (mode) {
            case SERVER_MODE:
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
                ServerController serverCtrl = new ServerController(server, serverDisplay);

                // Add the listeners
                server.addServerListener(serverCtrl);
                serverDisplay.addServerDisplayListener(serverCtrl);

                // Launch GUI.
                serverDisplay.launchDisplay();
                
                break;
            case CLIENT_MODE:
                if (args.length != 3) {
                    System.out.println("Did not provide valid argument length.");
                    printUsage();
                    exit();
                }
                // Get host and port from cmd line parameters
                String hostname = args[1];
                port = getPort(args[2]);
                
                // Create and alunch the client display parameters.
                ClientDisplay cd = new ClientDisplay(hostname, port);
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