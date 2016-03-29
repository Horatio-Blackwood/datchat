package datchat.client;

/**
 * 
 * @author adam
 */
public interface ClientListener {
    
    public void showMessage(String msg);
    
    public void connectionFailed();
}
