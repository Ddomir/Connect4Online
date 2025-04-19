import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;
import javafx.application.Platform;

public class Client extends Thread {
	private Socket socketClient;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private final Consumer<Message> callback;
	private boolean isConnected = false;

    Client(Consumer<Message> call) {
		callback = call;
	}

	public void run() {
		try {
			socketClient = new Socket("127.0.0.1", 5555);
			out = new ObjectOutputStream(socketClient.getOutputStream());
			in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
			isConnected = true;

			Platform.runLater(() -> callback.accept(
					new Message(MessageType.CONNECTION_SUCCESS, "System", "Connected to server")
			));

			while(isConnected) {
				try {
					Message message = (Message) in.readObject();
					callback.accept(message);
				} catch(Exception e) {
					closeConnection();
				}
			}
		} catch(Exception e) {
			closeConnection();
            Platform.runLater(() -> callback.accept(
                    new Message(MessageType.ERROR, "System", "Connection failed: " + e.getMessage())
            ));
        }
	}


	public void send(Message data) {
		if(out != null) {
			try {
				out.writeObject(data);
				out.flush();
			} catch (IOException e) {
				closeConnection();
			}
		}
	}

	private void closeConnection() {
		isConnected = false;
		try {
			if(socketClient != null) socketClient.close();
			if(in != null) in.close();
			if(out != null) out.close();
		} catch (IOException e) {
			System.err.println("Error closing connection");
		}
	}

	public boolean isConnected() {
		return isConnected;
	}
}