import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 42L;

    MessageType type;
    String sender;
    String content;
    List<String> userList;

    // For username validation
    public Message(MessageType type, String sender) {
        this.type = type;
        this.sender = sender;
    }

    // For user list updates
    public Message(MessageType type, List<String> userList) {
        this.type = type;
        this.userList = userList;
    }

    // For error messages
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }
}