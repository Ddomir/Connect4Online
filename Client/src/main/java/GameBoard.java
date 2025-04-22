import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class GameBoard extends GridPane {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private Circle[][] circles = new Circle[ROWS][COLS];
    private Rectangle[] indicators = new Rectangle[COLS];
    private Client client;
    private String username;
    private boolean gameOver = false;
    private Color playerColor = Color.RED;

    public GameBoard(Client client, String username) {
        this.client = client;
        this.username = username;

        // Create columns
        for (int col = 0; col < COLS; col++) {
            VBox column = new VBox();
            column.setMinWidth(60);
            column.setStyle("-fx-background-color: transparent;");

            // Circles for game board
            for (int row = 0; row < ROWS; row++) {
                Circle circle = new Circle(30, Color.WHITE);
                circles[row][col] = circle;
                column.getChildren().add(circle);
            }

            // Hover indicator (at bottom)
            Rectangle indicator = new Rectangle(60, 10, Color.TRANSPARENT);
            indicators[col] = indicator;
            column.getChildren().add(indicator);

            // Click handler for entire column
            final int finalCol = col;
            column.setOnMouseClicked(e -> {
                if (!gameOver && isValidMove(finalCol)) {
                    client.send(new Message(Message.Type.GAME_MOVE, username, new GameMoveData(finalCol, false, false)));
                }
            });

            // Hover handlers
            column.setOnMouseEntered(e -> {
                if (!gameOver && isValidMove(finalCol)) {
                    indicator.setFill(playerColor.deriveColor(1, 1, 1, 0.5)); // Semi-transparent
                }
            });
            column.setOnMouseExited(e -> {
                indicator.setFill(Color.TRANSPARENT);
            });

            add(column, col, 0);
        }

        setHgap(10);
        setVgap(10);
    }

    public void placePiece(int col, Color color) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (circles[row][col].getFill() == Color.WHITE) {
                circles[row][col].setFill(color);
                break;
            }
        }
    }

    public void resetBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                circles[row][col].setFill(Color.WHITE);
            }
        }
        gameOver = false;
        for (Rectangle indicator : indicators) {
            indicator.setFill(Color.TRANSPARENT);
        }
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
        // Hide indicators when game is over
        for (Rectangle indicator : indicators) {
            indicator.setFill(Color.TRANSPARENT);
        }
    }

    public void setPlayerColor(Color color) {
        this.playerColor = color;
    }

    private boolean isValidMove(int col) {
        return col >= 0 && col < COLS && circles[0][col].getFill() == Color.WHITE;
    }
}