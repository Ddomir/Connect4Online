import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Server {

    ArrayList<ClientThread> clients = new ArrayList<>();
    List<String> connectedUsers = new ArrayList<>();
    TheServer server;
    private final Consumer<Message> callback;

    Server(Consumer<Message> call) {
        callback = call;
        server = new TheServer();
        server.start();
    }

    public class TheServer extends Thread {
        public void run() {
            try (ServerSocket mysocket = new ServerSocket(5555)) {
                System.out.println("Server is waiting for a client!");

                while (true) {
                    ClientThread c = new ClientThread(mysocket.accept());
                    clients.add(c);
                    c.start();
                }
            } catch (Exception e) {
                callback.accept(new Message(MessageType.ERROR, "Server", "Server failed to launch"));
            }
        }
    }

    class ClientThread extends Thread {
        Socket connection;
        ObjectInputStream in;
        ObjectOutputStream out;
        String username;
        boolean authenticated = false;

        ClientThread(Socket s) {
            this.connection = s;
        }

        private void broadcastUserList() {
            Message userListMsg = new Message(MessageType.USER_LIST, connectedUsers);
            for (ClientThread t : clients) {
                try {
                    t.out.writeObject(userListMsg);
                } catch (Exception e) {
                    System.err.println("Error sending user list");
                }
            }
        }

        public void run() {
            try {
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
                connection.setTcpNoDelay(true);

                // Username validation loop
                while (!authenticated && !connection.isClosed()) {
                    Message validationMessage = (Message) in.readObject();

                    if (validationMessage.type == MessageType.USERNAME_VALIDATION) {
                        String requestedUsername = validationMessage.sender;

                        if (connectedUsers.contains(requestedUsername)) {
                            // Send rejection
                            out.writeObject(new Message(MessageType.USERNAME_TAKEN, "System", "Username '" + requestedUsername + "' is taken. Try another."));
                            out.flush();
                        } else {
                            // Accept username
                            username = requestedUsername;
                            connectedUsers.add(username);
                            authenticated = true;

                            out.writeObject(new Message(MessageType.USERNAME_ACCEPTED, "System", "Welcome " + username + "!"));
                            broadcastUserList();
                            callback.accept(new Message(MessageType.CONNECT, username, "User connected"));
                        }
                    }
                }

                // Main message loop
                while (authenticated) {
                    Message data = (Message) in.readObject();
                    callback.accept(data);

                    switch (data.type) {
                        case DISCONNECT:
                            throw new Exception("disconnect ohh nooooo");
                    }
                }

            } catch (Exception e) {
                // Handle disconnection
                if (authenticated) {
                    connectedUsers.remove(username);
                    broadcastUserList();
                    callback.accept(new Message(MessageType.DISCONNECT, username, "User disconnected"));
                }
                clients.remove(this);
            } finally {
                try {
                    connection.close();
                } catch (IOException e) {
                    System.out.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
}