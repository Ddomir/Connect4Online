import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;

    public enum Type {
        // Connection
        CONNECT_REQUEST, CONNECT_SUCCESS, CONNECT_ERROR,
        // Authentication
        AUTH_REQUEST, AUTH_SUCCESS, AUTH_ERROR,
        // Rooms
        ROOM_CREATE, ROOM_LIST, ROOM_JOIN, ROOM_UPDATE, ROOM_CANCEL,
        // Game
        GAME_START, GAME_MOVE, GAME_END,
        // Chat
        CHAT_SEND, CHAT_RECIEVE, CHAT_ERROR,
        // System
        PLAYER_UPDATE, DISCONNECT, SERVER_LOG
    }

    private final Type type;
    private final String sender;
    private final Object data;

    public Message(Type type) {
        this(type, "System", null);
    }

    public Message(Type type, String sender) {
        this(type, sender, null);
    }

    public Message(Type type, String sender, Object data) {
        this.type = type;
        this.sender = sender;
        this.data = data;
    }

    public Type getType() { return type; }
    public String getSender() { return sender; }
    public String getString() { return (String) data; }
    public List<String> getList() { return (List<String>) data; }
    public Object getData() { return data; }
}