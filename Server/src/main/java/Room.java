import java.util.ArrayList;
import java.util.List;

public class Room {
    final Server.ClientHandler host;
    public String id;
    private Server.ClientHandler guest;
    private List<String> chatHistory = new ArrayList<>();
    private GameLogic gameLogic = new GameLogic();

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
        chatHistory.add(formattedMessage);
        Message chatMsg = new Message(Message.Type.CHAT_RECIEVE, sender.username, message);
        host.send(chatMsg);
        if (guest != null) guest.send(chatMsg);
    }

    public void handleGameMove(int col, Server.ClientHandler player) {
        if (gameLogic.getCurrentPlayer() == (player == host ? 'R' : 'Y')) {
            if (gameLogic.makeMove(col)) {
                char winner = gameLogic.checkWinner();
                boolean isDraw = gameLogic.isDraw();
                GameMoveData moveData = new GameMoveData(col, winner != ' ', isDraw);
                Message moveMsg = new Message(Message.Type.GAME_MOVE, player.username, moveData);
                host.send(moveMsg);
                guest.send(moveMsg);

                if (winner != ' ' || isDraw) {
                    String endMessage = isDraw ? "Draw" : "Winner: " + (winner == 'R' ? host.username : guest.username);
                    Message endMsg = new Message(Message.Type.GAME_END, "System", endMessage);
                    host.send(endMsg);
                    guest.send(endMsg);
                } else {
                    gameLogic.switchPlayer();
                }
            }
        }
    }
}