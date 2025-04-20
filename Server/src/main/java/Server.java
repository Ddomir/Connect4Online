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

    private class ClientHandler extends Thread {
        private final Socket socket;
        private ObjectOutputStream out;
        private String username;

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
                    break;
                case ROOM_CREATE:
                    createRoom(this);
                    break;
                case ROOM_JOIN:
                    joinRoom(msg.getString());
                    break;
                case ROOM_LIST:
                    List<String> roomList = new ArrayList<>(rooms.keySet());
                    send(new Message(Message.Type.ROOM_LIST, "System", roomList));
                    break;
                case DISCONNECT:
                    disconnect();
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
            log("Room created: " + roomId);
            creator.send(new Message(Message.Type.ROOM_UPDATE, "System", "WAITING"));
            broadcastRoomList();
        }

        private void joinRoom(String roomId) {
            Room room = rooms.get(roomId);
            if (room != null && room.join(this)) {
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

        private void send(Message msg) {
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

    private class Room {
        private final String id;
        private final ClientHandler host;
        private ClientHandler guest;

        public Room(String id, ClientHandler host) {
            this.id = id;
            this.host = host;
        }

        public synchronized boolean join(ClientHandler client) {
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