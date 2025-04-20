import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class Client extends Thread {
	private final Consumer<Message> callback;
	private Socket socket;
	private ObjectOutputStream out;
	private String username;

	public Client(Consumer<Message> callback) {
		this.callback = callback;
	}

	public void run() {
		try {
			socket = new Socket("localhost", 5555);
			out = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			socket.setTcpNoDelay(true);
			System.out.println("Connected successfully");

			while (true) {
				Message msg = (Message) in.readObject();
				System.out.println("Received: " + msg.getType());
				callback.accept(msg);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			callback.accept(new Message(Message.Type.CONNECT_ERROR, "System", e.getMessage()));
		}
	}

	public void send(Message msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			callback.accept(new Message(Message.Type.CONNECT_ERROR, "System", "Connection lost"));
		}
	}

	public void authenticate(String username) {
		this.username = username;
		send(new Message(Message.Type.AUTH_REQUEST, username));
	}

	public void createRoom() {
		send(new Message(Message.Type.ROOM_CREATE, username));
	}

	public void joinRoom(String roomId) {
		send(new Message(Message.Type.ROOM_JOIN, username, roomId));
	}
}