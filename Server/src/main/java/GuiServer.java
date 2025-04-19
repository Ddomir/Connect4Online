import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application {

    Server serverConnection;
    ListView<String> listItems = new ListView<>();
    ListView<String> listUsers = new ListView<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        serverConnection = new Server(data -> {
            Platform.runLater(() -> {
                switch (data.type) {
                    case CONNECT:
                        listUsers.getItems().add(data.sender);
                        listItems.getItems().add(data.sender + " has joined!");
                        break;
                    case DISCONNECT:
                        listUsers.getItems().remove(data.sender);
                        listItems.getItems().add(data.sender + " has disconnected!");
                        break;
                    case ERROR:
                        listItems.getItems().add("Error: " + data.content);
                        break;
                }
            });
        });

        HBox lists = new HBox(20, listUsers, listItems);
        lists.setPadding(new Insets(20));

        BorderPane pane = new BorderPane();
        pane.setCenter(lists);
        pane.setStyle("-fx-font-family: 'serif'; -fx-padding: 20;");

        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.setScene(new Scene(pane, 600, 400));
        primaryStage.setTitle("Connect4 Server Console");
        primaryStage.show();
    }
}