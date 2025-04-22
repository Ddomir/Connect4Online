import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

public class GuiClient extends Application {
	private Client client;
	private String username;
	private VBox loginScreen;
	private VBox lobbyScreen;
	private VBox gameScreen;
	private ListView<String> roomList, chatList;
	private TextField roomField, chatMessageField;
	private Label statusLabel, gameLabel;
	private Button cancelBtn, sendChatBtn, exitBtn;
	private HBox mainContent;
	private GameBoard gameBoard;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		connectToServer();
		setupLoginScreen();
		setupLobbyScreen();
		setupGameScreen();

		StackPane root = new StackPane(loginScreen, lobbyScreen, gameScreen);
		stage.setScene(new Scene(root, 800, 400));
		stage.show();
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

		gameBoard = new GameBoard(client, username);
		exitBtn = new Button("Exit");
		exitBtn.setVisible(false);
		exitBtn.setOnAction(e -> showLobby());

		mainContent = new HBox(createChatBox(), gameBoard);
		mainContent.setVisible(false);

		gameScreen = new VBox(10, gameLabel, cancelBtn, mainContent, exitBtn);
		gameScreen.setVisible(false);
	}

	private VBox createChatBox() {
		chatList = new ListView<>();
		chatMessageField = new TextField();
		sendChatBtn = new Button("Send Chat");
		sendChatBtn.setOnAction(e -> {
			String message = chatMessageField.getText();
			if (!message.isEmpty()) {
				client.send(new Message(Message.Type.CHAT_SEND, username, message));
				chatMessageField.clear();
			}
		});

		return new VBox(20, chatList, new HBox(chatMessageField, sendChatBtn));
	}

	private void updateChatList(String msg) {
		chatList.getItems().add(msg);
		chatList.scrollTo(chatList.getItems().size() - 1);
	}

	private void showLobby() {
		loginScreen.setVisible(false);
		gameScreen.setVisible(false);
		lobbyScreen.setVisible(true);
		gameBoard.resetBoard();
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
		mainContent.setVisible(!waiting);
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
						statusLabel.setText("Room cancelled successfully");
					} else if (msg.getString().equals("NO_ROOM_TO_CANCEL")) {
						statusLabel.setText("You don’t have a room to cancel");
					}
					break;
				case GAME_START:
					String opponent = msg.getString();
					showGameScreen("Game started: " + username + " vs " + opponent, false);
					gameBoard.setPlayerColor(Color.RED); // Assume host is red; adjust if server assigns colors
					break;
				case CHAT_RECIEVE:
					String sender = msg.getSender();
					String chatMsg = msg.getString();
					updateChatList(sender + ": " + chatMsg);
					break;
				case GAME_MOVE:
					GameMoveData moveData = (GameMoveData) msg.getData();
					int col = moveData.getColumn();
					Color color = msg.getSender().equals(username) ? Color.RED : Color.YELLOW;
					gameBoard.placePiece(col, color);
					if (moveData.hasWinner() || moveData.isDraw()) {
						gameLabel.setText(moveData.isDraw() ? "Game Over: Draw" : "Winner: " + msg.getSender());
						gameBoard.setGameOver(true);
						exitBtn.setVisible(true);
					}
					break;
				case GAME_END:
					gameLabel.setText("Game Over: " + msg.getString());
					gameBoard.setGameOver(true);
					exitBtn.setVisible(true);
					break;
				default:
					statusLabel.setText("Unhandled message: " + msg.getType());
			}
		});
	}
}