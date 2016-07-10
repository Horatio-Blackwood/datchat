package datchat.server;

import datchat.Datchat;
import datchat.FileTools;

/**
 *
 * @author adam
 */
public class ServerLog implements ServerOutputHandler {
    
    private final String m_eventLog;
    private final String m_chatLog;
    
    public ServerLog() {
        long now = System.currentTimeMillis();
        m_eventLog = "./logs/" + Datchat.CHAT_FILE_FORMATTER.format(now) + "-dat-event.log";
        m_chatLog = "./logs/" + Datchat.CHAT_FILE_FORMATTER.format(now) + "-dat-chat.log";
    }

    @Override
    public void handleChatRoomMsg(String msg) {
        FileTools.log(m_chatLog, msg);
    }

    @Override
    public void handleEventMsg(String msg) {
        FileTools.log(m_eventLog, Datchat.CHAT_DATE_FORMATTER.format(System.currentTimeMillis()) + "  " + msg);
    }
    
}
