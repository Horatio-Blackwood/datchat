package datchat;

import datchat.server.Server;
import datchat.server.ServerController;
import datchat.server.ServerDisplay;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author adam
 */
public class Datchat {

    public static final String VERSION = "v0.1";
    public static final String DATE_CREATED = "27 March 2015";
    public static final int DEFAULT_PORT = 55200;
    
    public static final SimpleDateFormat CHAT_DATE_FORMATTER = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
    private static final List<String> VALID_MODE_ARGS = Arrays.asList("-s", "-c", "-sg", "-cg", "--server", "--client", "--serverg", "--clientg");
    
    /**
     * Runs DatChat in either server or client mode.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Verifies the right number of args.  Possible fourth arg for setting up an 'admin' password/key?
        if (args.length != 3) {
            System.out.println("Requires three args:  mode, hostname, and IP");
            System.out.println("Example:");
            System.out.println("    java -jar --server localhost 54200");
            System.out.println("    java -jar --client chatServer 54200");
            System.exit(1);
        }
        
        
        // validate/store args
        // MODE (server or client) - Arg index 0.
        String mode = args[0];
        if (!VALID_MODE_ARGS.contains(mode)) {
            System.out.println("Mode param must be one of the following:");
            for (String modeOption : VALID_MODE_ARGS) {
                System.out.println("   - " + modeOption);
            }
            System.exit(1);
        }
        
        // SERVER NAME (server mode) or HOSTNAME (client mode) - Arg index 1.
        String hostname = args[1];

        // PORT - Arg index 2.
        int port = 0;
        try {
            port = Integer.valueOf(args[2]);
        } catch (NumberFormatException nfe) {
            System.out.println("Port parameter must be an integral value.  Was:  " + args[2]);
            System.exit(1);
        }
        if (port < 1 || port > 65535) {
            System.out.println("Port must be between 1 and 65535.  Was:  " + port);
            System.exit(1);
        }
        
        
        // LAUNCH SERVER
        if (mode.equals("-s") || mode.equals("--server")) {
            // create a server object and start it

        }
        
        // LAUNCH SERVER (GUI MODE)
        if (mode.equals("-sg") || mode.equals("--serverg")) {
            Server s = new Server();
            ServerDisplay sd = new ServerDisplay(DEFAULT_PORT);
            ServerController controller = new ServerController(s, sd);

            // Add the listeners
            s.addServerListener(controller);
            sd.addServerDisplayListener(controller);

            // Launch GUI.
            sd.launchDisplay();            
        }
        
        // LAUNCH CLIENT
        if (mode.equals("-c") || mode.equals("--client")) {

        }
        
        // LAUNCH CLIENT (GUI MODE)
        if (mode.equals("-cg") || mode.equals("--clientg")) {

        }        
    }
}