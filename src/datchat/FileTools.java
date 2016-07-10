package datchat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author adam
 */
public class FileTools {

    public static void log(String filename, String msg) {
        List<String> msgs = new ArrayList<>();
        msgs.add(msg);
        log(filename, msgs);
    }
    
    public static void log(String filename, List<String> msg) {
        try {
            // Ensure file exists.
            File f = new File(filename);
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            
            // Log msg to file.
            Path file = Paths.get(filename);
            Files.write(file, msg, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
            
        } catch (IOException ioex) {
            System.out.println("Error writing log '" + msg + "' to file '" + filename + "'.");
            ioex.printStackTrace();
        }
    }
}
