import java.util.ArrayList;
import java.util.List;

public class Room {
    
    final Server.ClientHandler host;
    public String id;
    private Server.ClientHandler guest;
    private List<String> chatHistory = new ArrayList<>(); // Optional: store chat history

    public Room(String id, Server.ClientHandler host) {
        this.id = id;
        this.host = host;
    }

    public synchronized boolean join(Server.ClientHandler client) {
        if (guest == null && client != host) {
            guest = client;
            return true;
        }
        return false;
    }

    public void startGame() {
        host.send(new Message(Message.Type.GAME_START, "System", guest.username));
        guest.send(new Message(Message.Type.GAME_START, "System", host.username));
    }

    public void sendChatMessage(String message, Server.ClientHandler sender) {
        String formattedMessage = sender.username + ": " + message;
        chatHistory.add(formattedMessage); // Store in history (optional)
        Message chatMsg = new Message(Message.Type.CHAT_RECIEVE, sender.username, message);
        host.send(chatMsg);
        if (guest != null) {
            guest.send(chatMsg);
        }
    }
}