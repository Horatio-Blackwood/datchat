package datchat;

import java.io.Serializable;

/**
 * An object representing a Chat Message.
 * 
 * @author victor
 * @author adam
 */
public class ChatMessage implements Serializable {

    /** SVID */
    protected static final long serialVersionUID = 52L;

    /** The message type. */
    private final MessageType m_type;
    
    /** The message itself. */
    private final String m_message;

    /**
     * Creates a ChatMessage with the type and message supplied.
     * @param type the type of message.
     * @param message the message payload.
     */
    public ChatMessage(MessageType type, String message) {
        m_type = type;
        m_message = message;
    }

    /**
     * Returns the message type of this Message.
     * @return the message type of this Message.
     */
    public MessageType getType() {
        return m_type;
    }

    /**
     * Returns the text of the message to be delivered.
     * @return the text of the message to be delivered.
     */
    public String getMessage() {
        return m_message;
    }
}
