import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class GameBoard extends VBox {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private final Rectangle[][] holes = new Rectangle[ROWS][COLS];
    private final Rectangle[] indicators = new Rectangle[COLS];
    private final String username;
    private boolean gameOver = false;
    private Color playerColor = Color.web("#f16969");
    private Color opponentColor = Color.web("#71A0D9");
    private Label turnLabel;

    public GameBoard(Client client, String username) {
        this.username = username;

        turnLabel = new Label("Your turn");
        turnLabel.setId("turnLabel");
        turnLabel.setTextFill(playerColor);

        Pane boardPane = new Pane();

        double boardWidth = COLS * 50 + (COLS - 1) * 10 + 30;
        double boardHeight = ROWS * 40 + (ROWS - 1) * 10 + 30 + 8 + 8;

        Rectangle boardBase = new Rectangle(boardWidth, boardHeight);
        boardBase.setArcWidth(20);
        boardBase.setArcHeight(20);
        Shape board = boardBase;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                double x = 15 + col * (50 + 10) + 5;
                double y = 15 + row * (40 + 10);
                Rectangle holeShape = new Rectangle(x, y, 40, 40);
                holeShape.setArcWidth(8);
                holeShape.setArcHeight(8);
                board = Shape.subtract(board, holeShape);
            }
        }
        board.setFill(Color.web("#2c2c2c"));

        GridPane holeGrid = new GridPane();
        holeGrid.setHgap(10);
        holeGrid.setVgap(10);
        holeGrid.setPadding(new javafx.geometry.Insets(15));
        holeGrid.setStyle("-fx-background-color: transparent;");

        for (int col = 0; col < COLS; col++) {
            VBox column = new VBox(8);
            column.setMinWidth(50);
            column.setAlignment(javafx.geometry.Pos.CENTER);
            column.setStyle("-fx-background-color: transparent;");

            for (int row = 0; row < ROWS; row++) {
                Rectangle hole = new Rectangle(40, 40, Color.TRANSPARENT);
                hole.setStroke(Color.web("#505050"));
                hole.setStrokeWidth(2);
                hole.setArcWidth(8);
                hole.setArcHeight(8);
                holes[row][col] = hole;
                column.getChildren().add(hole);
            }

            Rectangle indicator = new Rectangle(50, 8, Color.TRANSPARENT);
            indicators[col] = indicator;
            column.getChildren().add(indicator);

            final int finalCol = col;
            column.setOnMouseClicked(e -> {
                if (!gameOver && isValidMove(finalCol)) {
                    client.send(new Message(Message.Type.GAME_MOVE, username, new GameMoveData(finalCol, false, false)));
                }
            });

            column.setOnMouseEntered(e -> {
                if (!gameOver && isValidMove(finalCol)) {
                    indicator.setFill(playerColor.deriveColor(1, 1, 1, 0.5));
                }
            });
            column.setOnMouseExited(e -> {
                indicator.setFill(Color.TRANSPARENT);
            });

            holeGrid.add(column, col, 0);
        }

        boardPane.getChildren().addAll(board, holeGrid);
        boardPane.setMaxSize(boardWidth, boardHeight);
        boardPane.setMinSize(boardWidth, boardHeight);
        boardPane.setPrefSize(boardWidth, boardHeight);

        setAlignment(Pos.CENTER);
        setSpacing(10);
        getChildren().addAll(turnLabel, boardPane);

        setMaxSize(boardWidth, boardHeight + 40);
        setMinSize(boardWidth, boardHeight + 40);
        setPrefSize(boardWidth, boardHeight + 40);
    }

    public void placePiece(int col, Color color) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (holes[row][col].getFill() == Color.TRANSPARENT) {
                holes[row][col].setFill(color);
                holes[row][col].setStroke(Color.TRANSPARENT);
                break;
            }
        }
    }

    public void resetBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                holes[row][col].setFill(Color.TRANSPARENT);
                holes[row][col].setStroke(Color.web("#505050"));
                holes[row][col].setStrokeWidth(2);
            }
        }
        gameOver = false;
        for (Rectangle indicator : indicators) {
            indicator.setFill(Color.TRANSPARENT);
        }
        setYourTurn(true);
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
        for (Rectangle indicator : indicators) {
            indicator.setFill(Color.TRANSPARENT);
        }
        turnLabel.setText(gameOver ? "Game Over" : "Your turn");
        turnLabel.setTextFill(gameOver ? Color.WHITE : playerColor);
    }

    public void setPlayerColor(Color color) {
        this.playerColor = color;
        this.opponentColor = color.equals(Color.web("#f16969")) ? Color.web("#71A0D9") : Color.web("#f16969");
        turnLabel.setTextFill(this.playerColor);
        System.out.println("Set player color to " + color + ", opponent color to " + opponentColor);
    }

    public void setYourTurn(boolean yourTurn) {
        if (!gameOver) {
            turnLabel.setText(yourTurn ? "Your turn" : "Opponent's turn");
            turnLabel.setTextFill(yourTurn ? playerColor : Color.web("#71A0D9"));
            System.out.println("Set turn for " + username + ": " + (yourTurn ? "Your turn (" + playerColor + ")" : "Opponent's turn (" + opponentColor + ")"));
        }
    }

    private boolean isValidMove(int col) {
        return col >= 0 && col < COLS && holes[0][col].getFill() == Color.TRANSPARENT;
    }
}