package datchat;

import java.io.Serializable;

/**
 *
 * @author adam
 */
public class UserStatus implements Serializable, Comparable<UserStatus> {

    public String user;
    public String hostname;
    public long sinceTime;
    public OnlineStatus status;
    
    public UserStatus(String username, String host, long since, OnlineStatus oStatus) {
        user = username;
        status = oStatus;
        hostname = host;
        sinceTime = since;
    }

    @Override
    public int compareTo(UserStatus t) {
        return this.user.compareTo(t.user);
    }
    
    public boolean isSameUser(String username, String host) {
        if (username.equals(user) && hostname.equals(host)) {
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "User Status:  " + user + " - " + status.toString() + " - " + hostname + " - " + sinceTime;
    }
}