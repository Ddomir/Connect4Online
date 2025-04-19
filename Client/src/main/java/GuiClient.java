import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GuiClient extends Application {
	private VBox loginScreen;
	private VBox mainScreen;
	private ListView<String> userListView;
	private String username;
	Client clientConnection;

	// Add main method
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		// Login Screen
		TextField usernameField = new TextField();
		Button submitBtn = new Button("Login");
		Label errorLabel = new Label();

		submitBtn.setOnAction(e -> {
			username = usernameField.getText().trim();
			if(username.isEmpty()) {
				errorLabel.setText("Username cannot be empty!");
				return;
			}

			if(clientConnection.isConnected()) {
				clientConnection.send(new Message(MessageType.USERNAME_VALIDATION, username));
			} else {
				errorLabel.setText("Not connected to server!");
			}
		});

		loginScreen = new VBox(10, usernameField, submitBtn, errorLabel);

		// Main Screen
		userListView = new ListView<>();
		mainScreen = new VBox(userListView);
		mainScreen.setVisible(false);

		StackPane root = new StackPane(loginScreen, mainScreen);

		clientConnection = new Client(data -> {
			Platform.runLater(() -> {
				switch(data.type) {
					case CONNECTION_SUCCESS:
						submitBtn.setDisable(false);
						errorLabel.setText("Connected!");
						break;
					case USERNAME_ACCEPTED:
						loginScreen.setVisible(false);
						mainScreen.setVisible(true);
						break;
					case ERROR:
						errorLabel.setText(data.content);
						submitBtn.setDisable(false);
						break;
					case USERNAME_TAKEN:
						errorLabel.setText("Username taken!");
						break;
					case USER_LIST:
						userListView.getItems().setAll(data.userList);
						break;
				}
			});
		});
		clientConnection.start();
		submitBtn.setDisable(true);
		errorLabel.setText("Connecting to server...");

		primaryStage.setScene(new Scene(root, 300, 400));
		primaryStage.setTitle("Connect4 Client");
		primaryStage.show();
	}
}