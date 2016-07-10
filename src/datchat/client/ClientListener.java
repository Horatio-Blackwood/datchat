package datchat.client;

import datchat.UserStatus;

/**
 * 
 * @author adam
 */
public interface ClientListener {
    
    public void showMessage(String msg);
    
    public void updateStatus(UserStatus userStat);
    
    public void connectionFailed();
}
