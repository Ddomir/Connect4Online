import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Server implements Runnable {
    private final Consumer<Message> callback;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Server(Consumer<Message> callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(5555)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            callback.accept(new Message(Message.Type.CONNECT_ERROR, "Server", e.getMessage()));
        }
    }

    public class ClientHandler extends Thread {
        private final Socket socket;
        private ObjectOutputStream out;
        String username;
        private Room currentRoom;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out = new ObjectOutputStream(socket.getOutputStream());
                socket.setTcpNoDelay(true);

                while (true) {
                    Message msg = (Message) in.readObject();
                    handleMessage(msg);
                }
            } catch (Exception e) {
                disconnect();
            }
        }

        private void handleMessage(Message msg) {
            switch (msg.getType()) {
                case AUTH_REQUEST:
                    handleAuth(msg);
                    log("User joined: " + msg.getSender());
                    break;
                case ROOM_CREATE:
                    createRoom(this);
                    break;
                case ROOM_JOIN:
                    joinRoom(msg.getString());
                    log(msg.getSender() + "joined room: " + msg.getString());
                    break;
                case ROOM_LIST:
                    List<String> roomList = new ArrayList<>(rooms.keySet());
                    send(new Message(Message.Type.ROOM_LIST, "System", roomList));
                    break;
                case ROOM_CANCEL:
                    Room roomToCancel = null;
                    for (Map.Entry<String, Room> entry : rooms.entrySet()) {
                        if (entry.getValue().host == this) {
                            roomToCancel = entry.getValue();
                            break;
                        }
                    }
                    if (roomToCancel != null) {
                        rooms.remove(roomToCancel.id);
                        broadcastRoomList();
                        send(new Message(Message.Type.ROOM_UPDATE, "System", "CANCELLED"));
                        log("Room cancelled: " + roomToCancel.id);
                    } else {
                        send(new Message(Message.Type.ROOM_UPDATE, "System", "NO_ROOM_TO_CANCEL"));
                    }
                    break;
                case DISCONNECT:
                    log("User disconnected: " + msg.getSender());
                    disconnect();
                    break;
                case CHAT_SEND:
                    if (currentRoom != null) {
                        currentRoom.sendChatMessage(msg.getString(), this);
                        log("Chat send: " + msg.getString());
                    } else {
                        send(new Message(Message.Type.CHAT_ERROR, "System", "Not in a room"));
                    }
                    break;
                case GAME_MOVE:
                    if (currentRoom != null) {
                        GameMoveData moveData = (GameMoveData) msg.getData();
                        currentRoom.handleGameMove(moveData.getColumn(), this);
                    }
                    break;
            }
        }

        private void handleAuth(Message msg) {
            String requestedUser = msg.getSender();
            if (clients.containsKey(requestedUser)) {
                send(new Message(Message.Type.AUTH_ERROR, "System", "Username taken"));
            } else {
                username = requestedUser;
                clients.put(username, this);
                send(new Message(Message.Type.AUTH_SUCCESS));
                broadcastPlayerList();
            }
        }

        private void createRoom(ClientHandler creator) {
            String roomId = creator.username + "'s Room";
            Room room = new Room(roomId, creator);
            rooms.put(roomId, room);
            creator.currentRoom = room;
            log("Room created: " + roomId);
            creator.send(new Message(Message.Type.ROOM_UPDATE, "System", "WAITING"));
            broadcastRoomList();
        }

        private void joinRoom(String roomId) {
            Room room = rooms.get(roomId);
            if (room != null && room.join(this)) {
                this.currentRoom = room;
                room.host.currentRoom = room;
                log(roomId + " - Game started");
                rooms.remove(roomId);
                broadcastRoomList();
                room.startGame();
            } else {
                send(new Message(Message.Type.ROOM_UPDATE, "System", "JOIN_ERROR"));
            }
        }

        private void disconnect() {
            if (username != null) {
                clients.remove(username);
                broadcastPlayerList();
            }
            try { socket.close(); } catch (IOException ignored) {}
        }

        void send(Message msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }

        private void log(String message) {
            callback.accept(new Message(Message.Type.SERVER_LOG, "Server", message));
        }
    }

    private void broadcastPlayerList() {
        List<String> players = new ArrayList<>(clients.keySet());
        Message msg = new Message(Message.Type.PLAYER_UPDATE, "System", players);
        clients.values().forEach(c -> c.send(msg));
    }

    private void broadcastRoomList() {
        List<String> roomList = new ArrayList<>(rooms.keySet());
        Message msg = new Message(Message.Type.ROOM_LIST, "System", roomList);
        clients.values().forEach(c -> c.send(msg));
    }
}