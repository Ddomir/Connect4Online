import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class GuiClient extends Application {
	private Client client;
	private String username;

	private VBox loginScreen;
	private VBox lobbyScreen;
	private VBox gameScreen;
	private ListView<String> roomList;
	private TextField roomField;
	private Label statusLabel, gameLabel;
	private Button cancelBtn;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		setupLoginScreen();
		setupLobbyScreen();
		setupGameScreen();

		StackPane root = new StackPane(loginScreen, lobbyScreen, gameScreen);
		stage.setScene(new Scene(root, 400, 400));
		stage.show();

		connectToServer();
	}

	private void connectToServer() {
		client = new Client(this::handleMessage);
		client.start();
	}

	private void setupLoginScreen() {
		TextField userField = new TextField();
		Button loginBtn = new Button("Login");
		statusLabel = new Label();

		loginBtn.setOnAction(e -> {
			username = userField.getText();
			client.authenticate(username);
		});

		loginScreen = new VBox(10, userField, loginBtn, statusLabel);
	}

	private void setupLobbyScreen() {
		roomList = new ListView<>();
		Button createBtn = new Button("Create Room");
		Button joinBtn = new Button("Join Room");
		Button refreshBtn = new Button("Refresh");

		createBtn.setOnAction(e -> client.createRoom());
		joinBtn.setOnAction(e -> {
			String selectedRoom = roomList.getSelectionModel().getSelectedItem();
			if (selectedRoom != null) {
				client.joinRoom(selectedRoom);
			} else {
				statusLabel.setText("Error: Select a room to join");
			}
		});
		refreshBtn.setOnAction(e -> client.send(new Message(Message.Type.ROOM_LIST)));

		lobbyScreen = new VBox(10, roomList, new HBox(10, createBtn, joinBtn, refreshBtn));
		lobbyScreen.setVisible(false);
	}

	private void setupGameScreen() {
		gameLabel = new Label("Waiting");
		cancelBtn = new Button("Cancel");
		cancelBtn.setOnAction(e -> {
			client.send(new Message(Message.Type.ROOM_CANCEL, username));
		});
		gameScreen = new VBox(10, gameLabel, cancelBtn);
		gameScreen.setVisible(false);
	}

	private void showLobby() {
		loginScreen.setVisible(false);
		lobbyScreen.setVisible(true);
		client.send(new Message(Message.Type.ROOM_LIST));
	}

	private void updateRoomList(List<String> rooms) {
		roomList.getItems().setAll(rooms);
	}

	private void showGameScreen(String message, boolean waiting) {
		loginScreen.setVisible(false);
		lobbyScreen.setVisible(false);
		gameScreen.setVisible(true);
		gameLabel.setText(message);
		cancelBtn.setVisible(waiting);
	}


	private void handleMessage(Message msg) {
		System.out.println("Handling message: " + msg.getType());
		Platform.runLater(() -> {
			switch (msg.getType()) {
				case AUTH_SUCCESS:
					showLobby();
					statusLabel.setText("Login successful");
					break;
				case CONNECT_ERROR:
					statusLabel.setText("Connection error: " + msg.getString());
					break;
				case AUTH_ERROR:
					statusLabel.setText("Login failed: " + msg.getString());
					break;
				case ROOM_LIST:
					updateRoomList(msg.getList());
					break;
				case ROOM_UPDATE:
					if (msg.getString().equals("WAITING")) {
						showGameScreen("Waiting for another player...", true);
					} else if (msg.getString().equals("CANCELLED")) {
						showLobby();
					}
					break;
				case GAME_START:
					String opponent = msg.getString();
					showGameScreen("Game started: " + username + " vs " + opponent, false);
					break;
				default:
					statusLabel.setText("Unhandled message: " + msg.getType());
			}
		});
	}
}