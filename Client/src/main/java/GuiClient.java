import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

public class GuiClient extends Application {
	private Client client;
	private String username;
	private VBox lobbyScreenV, gameScreen, loginScreenV, roomListContainer;
	private ListView<String> chatList;
	private TextField chatMessageField;
	private Label statusLabel, gameLabel;
	private Button cancelBtn, sendChatBtn, exitBtn;
	private HBox mainContent, loginScreen, lobbyScreen;
	private GameBoard gameBoard;
	private BorderPane waitPage;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		connectToServer();
		setupLoginScreen();
		setupLobbyScreen();
		setupGameScreen();

		// Background Gif
//		Image gif = new Image("/images/starsbg.gif");
//		ImageView bgView = new ImageView(gif);
//		bgView.setFitWidth(800);
//		bgView.setFitHeight(600);
//		bgView.setPreserveRatio(false);


		StackPane root = new StackPane(loginScreen, lobbyScreen, gameScreen);
		Scene scene = new Scene(root, 800, 600);
		scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/fonts.css")).toExternalForm());
		scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/loginScreen.css")).toExternalForm());
		scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/lobbyScreen.css")).toExternalForm());
		scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/gameScreen.css")).toExternalForm());

		stage.setOnCloseRequest(e -> {
			if (client != null && username != null && gameLabel.getText().startsWith("Waiting")) {
				client.send(new Message(Message.Type.ROOM_CANCEL, username));
				client.send(new Message(Message.Type.DISCONNECT, username));
			}
			Platform.exit();
			System.exit(0);
		});

		stage.setScene(scene);
		stage.show();
	}

	private void connectToServer() {
		client = new Client(this::handleMessage);
		client.start();
	}

	private void setupLoginScreen() {
		Label loginTitle1 = new Label("Connect ");
		loginTitle1.setId("titleRed");
		Label loginTitle2 = new Label("4");
		loginTitle2.setId("titleBlue");
		HBox titleBox = new HBox(loginTitle1, loginTitle2);
		TextField userField = new TextField();
		userField.setPromptText("Username");
		Button loginBtn = new Button("Login");
		statusLabel = new Label();

		loginBtn.setOnAction(e -> {
			username = userField.getText();
			client.authenticate(username);
		});

		loginScreenV = new VBox(10, titleBox, userField, loginBtn, statusLabel);
		loginScreenV.setStyle("-fx-alignment: center;");
		loginScreen = new HBox(loginScreenV);
		loginScreen.setId("loginScreenRoot");
	}

	private void setupLobbyScreen() {
		Label titleLabel = new Label("Online Rooms");
		titleLabel.setId("lobbyTitle");

		roomListContainer = new VBox(10);
		roomListContainer.setPadding(new Insets(2));
		roomListContainer.setId("roomListContainer");

		ScrollPane scrollPane = new ScrollPane(roomListContainer);
		scrollPane.setFitToWidth(true);
		scrollPane.setId("roomListScroll");

		Button createBtn = new Button("Create Room");
		Button joinBtn = new Button("Join Room");
		Button refreshBtn = new Button("Refresh");

		createBtn.setOnAction(e -> client.createRoom());

		joinBtn.setOnAction(e -> {
			Button selectedBtn = (Button) roomListContainer.getChildren()
					.stream()
					.filter(n -> n.getStyleClass().contains("room-button-selected"))
					.findFirst().orElse(null);
			if (selectedBtn != null) {
				client.joinRoom(selectedBtn.getText());
			} else {
				statusLabel.setText("Error: Select a room to join");
			}
		});
		refreshBtn.setOnAction(e -> client.send(new Message(Message.Type.ROOM_LIST)));

		HBox buttonBox = new HBox(20, createBtn, joinBtn, refreshBtn);
		buttonBox.setAlignment(Pos.CENTER);

		VBox listAndButtons = new VBox(20, scrollPane, buttonBox);
		listAndButtons.setAlignment(Pos.CENTER);
		VBox.setVgrow(scrollPane, Priority.ALWAYS);

		BorderPane content = new BorderPane();
		content.setTop(titleLabel);
		content.setCenter(listAndButtons);
		BorderPane.setAlignment(titleLabel, Pos.TOP_LEFT);

		lobbyScreen = new HBox(content);
		HBox.setHgrow(content, Priority.ALWAYS);
		content.setMaxWidth(Double.MAX_VALUE);

		lobbyScreen.setId("lobbyScreenRoot");
		lobbyScreen.setVisible(false);
	}

	private void setupGameScreen() {
		gameLabel = new Label("Waiting for opponent to join...");
		gameLabel.setId("waitLabel");
		cancelBtn = new Button("Cancel");
		cancelBtn.setOnAction(e -> {
			client.send(new Message(Message.Type.ROOM_CANCEL, username));
		});

		VBox waitContent = new VBox(20, gameLabel, cancelBtn);
		waitContent.setAlignment(Pos.CENTER);
		waitContent.setId("waitPageRoot");

		waitPage = new BorderPane(waitContent);
		waitPage.setPrefSize(800, 600);
		waitPage.setId("waitPage");

		gameBoard = new GameBoard(client, username);
		mainContent = new HBox(createChatBox(), gameBoard);
		mainContent.setVisible(false);

		exitBtn = new Button("Exit");
		exitBtn.setVisible(false);
		exitBtn.setOnAction(e -> {
			showLobby();
			exitBtn.setVisible(false);
		});

		StackPane gameStateStack = new StackPane(waitPage, mainContent);

		gameScreen = new VBox(10, gameStateStack, exitBtn);
		gameScreen.setAlignment(Pos.CENTER);
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
		roomListContainer.getChildren().clear();
		for (String room : rooms) {
			Button roomBtn = new Button(room);
			roomBtn.getStyleClass().add("room-button");
			roomBtn.setMaxWidth(Double.MAX_VALUE);
			roomBtn.setOnAction(e -> {
				// Deselect all others
				roomListContainer.getChildren().forEach(node -> node.getStyleClass().remove("room-button-selected"));
				roomBtn.getStyleClass().add("room-button-selected");
			});
			roomListContainer.getChildren().add(roomBtn);
		}
	}


	private void showGameScreen(String message, boolean waiting) {
		loginScreen.setVisible(false);
		lobbyScreen.setVisible(false);
		gameScreen.setVisible(true);
		waitPage.setVisible(waiting);
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