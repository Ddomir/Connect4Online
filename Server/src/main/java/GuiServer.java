import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GuiServer extends Application {
    private Server server;
    private ListView<String> logView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        logView = new ListView<>();
        server = new Server(msg -> {
            Platform.runLater(() -> {
                switch (msg.getType()) {
                    case SERVER_LOG:
                        logView.getItems().add(msg.getString());
                        break;
                    case PLAYER_UPDATE:
                        logView.getItems().add("Players online: " + msg.getList().size());
                        break;
                }
            });
        });
        // Start the server in a separate thread
        new Thread(server).start();

        Label title = new Label("Server");

        VBox root = new VBox(title, logView);
        stage.setScene(new Scene(root, 400, 400));
        stage.setTitle("Server Console");
        stage.show();
    }
}