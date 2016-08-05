package datchat;

/**
 * An enumeration of Message Types.
 * @author adam
 */
public enum MessageType {
    /** A text message sent from clients to the server, or from the server to the client. */
    CHAT_MESSAGE,
    /** A request to disconnect from the server only sent from clients to the server. */
    LOGOUT,
    /** A status message about a user, only sent from servers to clients. */
    USER_STATUS;
}
